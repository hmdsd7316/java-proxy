package com.sid.proxy.sockets;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONObject;

import com.sid.proxy.alter.AlterRR;
import com.sid.proxy.interfaces.ProxySever;
import com.sid.proxy.interfaces.Request;
import com.sid.proxy.interfaces.ServerRequest;

public class ProxyToServer implements Runnable {

	private Socket proxySocket = null;
	private Socket serverSocket = null;
	public String destinationEndpoint = null;

	public ProxyToServer(Socket proxySocket, String destinationEndpoint) {
		this.proxySocket = proxySocket;
		this.destinationEndpoint = destinationEndpoint;
	}

	@Override
	public void run() {
		ByteArrayOutputStream baisHeader = new ByteArrayOutputStream();
		ByteArrayOutputStream baisBody = new ByteArrayOutputStream();
		String bodyString = null;

		BufferedReader eclipseInReader = null;
		try {

			// Read Headers ===
			eclipseInReader = new BufferedReader(new InputStreamReader(proxySocket.getInputStream(), StandardCharsets.UTF_8));

			// Process Headers ===
			String headerString = readHeader(eclipseInReader);
			RequestHeader header = RequestHeader.parse(headerString);

			// Read Body ===
			bodyString = readBody(eclipseInReader, header);

			if ((header.address.contains("/wd/hub/session"))) {
				this.forwardToServer(header, headerString, bodyString);
			}
			if (header.address.contains("/wd/hub/status")) {
				returnHubStatusResponse();
			}

			System.out.println(Thread.currentThread().getName() + " " + "Proxy request & response completed.");
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
			returnHttpErrorResponse(e);
		} finally {
			cleanResources(baisHeader, baisBody, eclipseInReader);
		}
	}

	private String readHeader(BufferedReader eclipseInReader) throws IOException, ParseException {
		ByteArrayOutputStream header = new ByteArrayOutputStream();
		String line = null;
		while ((line = eclipseInReader.readLine()) != null) {
			line = line + "\r\n";

			byte[] lineBytes = (line).getBytes(StandardCharsets.UTF_8);
			header.write(lineBytes);

			// System.out.print(" >> " + line);

			if (line.isEmpty() || line.contentEquals("\r\n"))
				break;
		}
		String value = new String(header.toByteArray(), StandardCharsets.UTF_8);
		System.out.println("Header:");
		System.out.println(value);
		return value;

	}

	private String readBody(BufferedReader eclipseInReader, RequestHeader header) throws IOException {
		ByteArrayOutputStream bodyTemp = new ByteArrayOutputStream();
		bodyTemp.flush();
		while (header.Protocol == HttpProtocol.POST && true) {
			CharBuffer buffer = CharBuffer.allocate(ProxyThread.BUFFER_SIZE);
			int charactersRead = eclipseInReader.read(buffer);

			String bytesToString = new String(buffer.array());

			bodyTemp.write(bytesToString.getBytes(StandardCharsets.UTF_8), 0, bytesToString.substring(0, charactersRead).getBytes(StandardCharsets.UTF_8).length);
			System.out.println("  >> " + bytesToString);

			if (charactersRead < ProxyThread.BUFFER_SIZE) {
				break;
			}
		}
		System.out.println("Request Body: " + bodyTemp.toString());
		return bodyTemp.toString();
	}

	private void forwardToServer(RequestHeader header, String headerString, String bodyString) throws UnknownHostException, IOException, InterruptedException {

		byte[] headerByteArray = headerString.getBytes(StandardCharsets.UTF_8);
		byte[] bodyByteArray = bodyString.getBytes(StandardCharsets.UTF_8);

//			if (pCloudyAppiumSessionInfo.pCloudyAppiumHub.getProtocol().equalsIgnoreCase("https")) {
//				this.pCloudyAppiumHub = SSLSocketFactory.getDefault().createSocket(pCloudyAppiumSessionInfo.pCloudyAppiumHub.getHost(), 443);
//
//			} else {
//				this.pCloudyAppiumHub = new Socket("localhost", 4724);
//			}

		this.serverSocket = new Socket("localhost", 4723);

		ProxySever clientCode = new AlterRR();
		Request request = new ServerRequest();
		request.setBody(new String(bodyByteArray));
		request.setHeader(new String(headerByteArray));
		Request alterRequest = clientCode.request(request);

		// Write Headers to Appium Hub
		// ==================================================================================
		this.serverSocket.getOutputStream().write(alterRequest.getHeader().getBytes(StandardCharsets.UTF_8));

		if (header.Protocol == HttpProtocol.POST || header.Protocol == HttpProtocol.DELETE) {
			// Write Body to Appium Hub
			// ==================================================================================
			this.serverSocket.getOutputStream().write(alterRequest.getBody().getBytes(StandardCharsets.UTF_8));
		}

		// Start Out Thread to Appium Hub
		// ================================================================================
		try (ProxyToClient a2e = new ProxyToClient(serverSocket.getInputStream(), proxySocket.getOutputStream(), header, clientCode)) {
			Thread t2 = new Thread(a2e);
			t2.start();

			serverSocket.getOutputStream().flush();
			t2.join();

		}

	}

	private void returnHttpErrorResponse(Exception ex) {

		System.out.println("Sending Http Error to Eclipse");

		// String body = "{\"status\":33,\"value\":{\"message\":\"" +
		// ex.getClass().getName() + " : " + ex.getMessage() +
		// "\"},\"sessionId\":null}";
		org.json.JSONObject _jsonBody = new JSONObject();
		_jsonBody.put("status", 33);
		_jsonBody.put("value", new JSONObject().put("message", ex.getClass().getName() + " : " + ex.getMessage()));
		_jsonBody.put("sessionId", JSONObject.NULL);

		DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));

		String header = "HTTP/1.1 500 Internal Server Error\n";
		header += "Date: " + df.format(new Date()) + "\n";
		header += "access-control-allow-origin: *\n";
		header += "access-control-allow-methods: GET,POST,PUT,OPTIONS,DELETE\n";
		header += "access-control-allow-headers: origin, content-type, accept\n";
		header += "vary: X-HTTP-Method-Override\n";
		header += "content-type: application/json; charset=utf-8\n";
		header += "content-length: " + _jsonBody.toString().length() + "\n";
		header += "connection: keep-alive" + "\n";

		// header = header.replace("#length#", _jsonBody.toString().length() + "");

		try {
			String httpErrorMsg = header + "\n" + _jsonBody.toString();
			// System.out.println("HttpErrorMsg: \n'" + httpErrorMsg + "'");
			proxySocket.getOutputStream().write(httpErrorMsg.getBytes(StandardCharsets.UTF_8));
			proxySocket.getOutputStream().flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void returnHubStatusResponse() {

		System.out.println("Sending Hub Status to Eclipse");
		DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));

		String header = "HTTP/1.1 200 OK\n" + "X-Powered-By: Express\n" + "access-control-allow-origin: *\n" + "access-control-allow-methods: GET,POST,PUT,OPTIONS,DELETE\n"
				+ "access-control-allow-headers: origin, content-type, accept\n" + "vary: X-HTTP-Method-Override\n" + "content-type: application/json; charset=utf-8\n" + "content-length: #length#\n"
				+ "ETag: W/\"4c-ls0DQ39q6iMpa8keP7gdpBWjP20\"" + "\n" + "Date: " + df.format(new Date()) + "\n" + "connection: close" + "\n";

		String body = "{\"status\":0,\"value\":{\"build\":{\"version\":\"1.7.1\",\"revision\":null}},\"sessionId\":null}";

		header = header.replace("#length#", body.length() + "");

		try {
			String httpErrorMsg = header + "\n" + body;
			System.out.println("HttpErrorMsg: \n'" + httpErrorMsg + "'");
			proxySocket.getOutputStream().write(httpErrorMsg.getBytes(StandardCharsets.UTF_8));
			proxySocket.getOutputStream().flush();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void cleanResources(ByteArrayOutputStream baisHeader, ByteArrayOutputStream baisBody, BufferedReader eclipseInReader) {
		try {
			baisHeader.close();
			baisBody.close();
			if (serverSocket != null)
				serverSocket.close();

			if (eclipseInReader != null)
				eclipseInReader.close();

			proxySocket.close();
		} catch (IOException e) {
			// this should never happen
			e.printStackTrace();
		}
	}

}
