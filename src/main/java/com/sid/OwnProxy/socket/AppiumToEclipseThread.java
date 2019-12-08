package com.sid.OwnProxy.socket;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.sid.OwnProxy.interfaces.ProxySever;
import com.sid.OwnProxy.interfaces.Response;
import com.sid.OwnProxy.server.ServerResponse;

public class AppiumToEclipseThread implements Runnable, Closeable {

	InputStream appiumIn;
	OutputStream eclipseOut;
	ParsedRequestHeader header;
	ProxySever proxy;
	
	private StringBuffer sb = new StringBuffer();

	public AppiumToEclipseThread(InputStream appiumIn, OutputStream eclipseOut, ProxySever proxy) {
		this.appiumIn = appiumIn;
		this.eclipseOut = eclipseOut;
		this.proxy = proxy;
	}

	@Override
	public void run() {
		ByteArrayOutputStream baisHeader = new ByteArrayOutputStream();
		ByteArrayOutputStream baisBody = new ByteArrayOutputStream();
		try {
			// Lets read from Eclipse Socket and write the request to Appium Socket
			BufferedReader br = new BufferedReader(new InputStreamReader(appiumIn, StandardCharsets.UTF_8));
			// Read Response Headers ====================
			baisHeader = readHeaders(br);

			// Read Response Body ====================
			baisBody = readBody(br);
			
			// Get Altered Response
			Response response = new ServerResponse();
			response.setBody(baisBody.toString());
			response.setHeader(baisHeader.toString());
			Response alterResponse = this.proxy.response(response);
			
			String header = alterResponse.getHeader();
			String body = alterResponse.getBody();
			
			// Write Headers
			eclipseOut.write(header.getBytes(StandardCharsets.UTF_8));
			eclipseOut.flush();

			// Write Body
			eclipseOut.write(body.getBytes(StandardCharsets.UTF_8));
			eclipseOut.flush();

			System.out.println();
			System.out.println("AppiumToEclipseThread completed.");

		} catch (Exception e) {
			returnHttpErrorResponse(e);
			System.out.println("releasing session cause of server error.");
		} finally {
			try {
				eclipseOut.flush();
				eclipseOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private ByteArrayOutputStream readHeaders(BufferedReader br) throws IOException {
		String line = null;
		ByteArrayOutputStream header = new ByteArrayOutputStream();
		while ((line = br.readLine()) != null) {
			line = line + "\r\n";

			sb.append(line);

			byte[] lineBytes = (line).getBytes(StandardCharsets.UTF_8);
			System.out.print("  <<  " + line);
			header.write(lineBytes);
			// eclipseOut.write(lineBytes);

			if (line.isEmpty() || line.contentEquals("\r\n"))
				break;
		}
		return header;
	}

	private ByteArrayOutputStream readBody(BufferedReader br) throws ParseException, IOException{
		int contentLength = this.getContentLength(sb.toString());
		int totalBytesRead = 0;
		int totalSubStringBytesRead = 0;
		ByteArrayOutputStream body = new ByteArrayOutputStream();

		while (true) {
			CharBuffer buffer = CharBuffer.allocate(ProxyThread.BUFFER_SIZE);
			int bytesRead = br.read(buffer);
			totalBytesRead += bytesRead;

			String bytesToString = new String(buffer.array(), 0, bytesRead);

			if (bytesToString.contains("{\"error\":\"error!!\"}")) {
				bytesToString = bytesToString.replaceAll("\\d", "").replaceAll("\n", "").replaceAll("\\s", "");
			}

			if (totalBytesRead < ProxyThread.BUFFER_SIZE)
				System.out.print("  <<  " + bytesToString);

			sb.append(bytesToString);
			// add new session id first time

			body.write(bytesToString.getBytes(StandardCharsets.UTF_8), 0, bytesToString.substring(0, bytesRead).getBytes(StandardCharsets.UTF_8).length);

			totalSubStringBytesRead += bytesToString.substring(0, bytesRead).getBytes(StandardCharsets.UTF_8).length;

			if (totalSubStringBytesRead >= contentLength)
				break;

			if (bytesToString.substring(0, bytesRead).getBytes(StandardCharsets.UTF_8).length >= contentLength)
				break;

			if (contentLength == -1 && bytesRead < ProxyThread.BUFFER_SIZE)
				break;
		}
		return body;
	}

	private int getContentLength(String header) {
		Pattern p = Pattern.compile("(?<=content-length:\\ ).+", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(header);
		if (m.find()) {
			return Integer.parseInt(m.group());
		} else {
			return -1;
		}
	}

	public String getResponse() {
		return sb.toString();
	}

	@Override
	public void close() throws IOException {
		this.sb = null;
		this.appiumIn = null;
		this.eclipseOut = null;

	}

	private void returnHttpErrorResponse(Exception ex) {

		DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));

		String header = "HTTP/1.1 500 Internal Server Error\n" + "Date: " + df.format(new Date()) + "\n" + "access-control-allow-origin: *\n"
				+ "access-control-allow-methods: GET,POST,PUT,OPTIONS,DELETE\n" + "access-control-allow-headers: origin, content-type, accept\n" + "vary: X-HTTP-Method-Override\n"
				+ "content-type: application/json; charset=utf-8\n" + "content-length: #length#\n" + "connection: keep-alive" + "\n";

		// String body = "{\"status\":33,\"value\":{\"message\":\"" + ex.getClass().getName() + " : " + ex.getMessage() + "\"},\"sessionId\":null}";
		org.json.JSONObject _jsonBody = new JSONObject();
		_jsonBody.put("status", 33);
		_jsonBody.put("value", new JSONObject().put("message", "Appium Server Error due to some reason please check Hub/AppiumStdErr logs for more Information"));
		_jsonBody.put("sessionId", JSONObject.NULL);

		header = header.replace("#length#", _jsonBody.toString().length() + "");

		try {
			String httpErrorMsg = header + "\n" + _jsonBody.toString();
			System.out.println("HttpErrorMsg: \n'" + httpErrorMsg + "'");

			eclipseOut.write(httpErrorMsg.getBytes(StandardCharsets.UTF_8));
			eclipseOut.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
