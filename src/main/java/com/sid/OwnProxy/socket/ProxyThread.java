package com.sid.OwnProxy.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.SSLSocketFactory;

import com.sid.OwnProxy.interfaces.ProxySever;
import com.sid.OwnProxy.interfaces.Request;
import com.sid.OwnProxy.interfaces.Response;
import com.sid.OwnProxy.server.ServerRequest;
import com.sid.OwnProxy.server.ServerResponse;

public class ProxyThread extends Thread {
	private Socket socketObject = null;
	static final int BUFFER_SIZE = 1024;
	private String header;
	private String body;
	private ProxySever proxy;
	private URL serverEndpoint;
	private Request alterRequest;
	private Response alterResponse;

	public ProxyThread(Socket eclipseSocket, ProxySever proxy, URL serverEndpoint) {
		this.socketObject = eclipseSocket;
		this.proxy = proxy;
		this.serverEndpoint = serverEndpoint;
	}

	public void run() {
		System.out.println("=================== New Thread started ===============" + Thread.currentThread().getName());
		try {
			this.mainRunner();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void mainRunner() throws IOException, ParseException, InterruptedException {
		this.setHeaderBody();
		this.getAlteredRequest();
		this.forwardToDestination();
		this.returnAlterdReposnse();

	}

	private void setHeaderBody() throws IOException, ParseException {
		this.header = "";
		BufferedReader eclipseInReader = new BufferedReader(new InputStreamReader(this.socketObject.getInputStream(), StandardCharsets.UTF_8));
		String line = null;
		while ((line = eclipseInReader.readLine()) != null) {
			line = line + "\r\n";
			if (line.isEmpty() || line.contentEquals("\r\n")) {
				System.out.println("Header reddddd: " + this.header);
				this.setBody(eclipseInReader);
				break;
			}

			this.header += line;
		}
		this.header.trim();
	}

	private void setBody(BufferedReader eclipseInReader) throws IOException {
		String line = null;
		this.body = "";
		System.out.println("Body.....reding");
		if(this.header.contains("GET")) {
			System.out.println("skipping reading body");
			return;
		}
		while (true) {
			CharBuffer buffer = CharBuffer.allocate(ProxyThread.BUFFER_SIZE);
			int charactersRead = eclipseInReader.read(buffer);
			line = new String(buffer.array());
			this.body += line;
			if (charactersRead < ProxyThread.BUFFER_SIZE) {
				break;
			}

		}
		this.body.trim();
		System.out.println("Ended Happ");
	}

	private void getAlteredRequest() {
		Request request = new ServerRequest();
		request.setBody(this.body);
		request.setHeader(this.header);
		this.alterRequest = this.proxy.request(request);
	}

//	private String forwardToSpringBoot() throws MalformedURLException, UnknownHostException {
//		this.cloudEndpoint = this.getCloudEndPoint();
//		return this.doPostApi();
//	}

//	private URL getCloudEndPoint() throws MalformedURLException, UnknownHostException {
//
//		URL endpoint = CloudBoot.getInstance().getEndpoint();
//		System.out.println(endpoint);
//		return endpoint;
//	}

//	@SuppressWarnings("unchecked")
//	private String getHeadersJsonString() {
//		JSONObject jsonObject = new JSONObject();
//		String value = this.header;
////		String value = "POST /session HTTP/1.1\n" + "User-Agent: selenium/3.141.59 (java unix)\n"
////				+ "Content-Type: application/json; charset=utf-8\n" + "Content-Length: 1097\n"
////				+ "Host: 127.0.1.1:9400\n" + "Connection: Keep-Alive\n" + "Accept-Encoding: gzip";
//		String[] lines = value.split("\n");
//
//		jsonObject.put("type", lines[0].split(" ")[0]);
//		jsonObject.put("endpoint", lines[0].split(" ")[1]);
//		jsonObject.put("protocol", lines[0].split(" ")[2].split("/")[0]);
//
//		for (int i = 1; i < lines.length; i++) {
//			String[] split = lines[i].split(": ");
//			jsonObject.put(split[0], split[1]);
//		}
//		System.out.println(jsonObject.toJSONString());
//		return jsonObject.toJSONString();
//	}

//	private void callGETApi() {
//
//		try {
//			RestTemplate restTemplate = new RestTemplate();
//			JSONObject result = restTemplate.getForObject("http://localhost:9700/status", JSONObject.class);
//			System.out.println("RESSS: " + result.toJSONString());
//		} catch (Exception e) {
//			System.out.println("Server not started. need to start.");
//		}
//	}

	private void returnAlterdReposnse() {
		Response response = new ServerResponse();
		response.setBody(body);
		response.setHeader(header);
		Response alteredResponse = this.proxy.response(response);

		System.out.println("Altered Response: " + alteredResponse.getBody());
		System.out.println("Sending Hub Status to Eclipse");
		DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));

		String header = "HTTP/1.1 200 OK\n" + "X-Powered-By: Express\n" + "access-control-allow-origin: *\n" + "access-control-allow-methods: GET,POST,PUT,OPTIONS,DELETE\n"
				+ "access-control-allow-headers: origin, content-type, accept\n" + "vary: X-HTTP-Method-Override\n" + "content-type: application/json; charset=utf-8\n" + "content-length: #length#\n"
				+ "ETag: W/\"4c-ls0DQ39q6iMpa8keP7gdpBWjP20\"" + "\n" + "Date: " + df.format(new Date()) + "\n" + "connection: close" + "\n";

//		String body = "{\"status\":0,\"value\":{\"build\":{\"version\":\"1.7.1\",\"revision\":null}},\"sessionId\":null}";

		String body = alteredResponse.getBody();
		header = header.replace("#length#", body.length() + "");

		try {

			String httpErrorMsg = header + "\n" + body;
			System.out.println("HttpErrorMsg: \n'" + httpErrorMsg + "'");
			this.socketObject.getOutputStream().write(httpErrorMsg.getBytes(StandardCharsets.UTF_8));
			this.socketObject.getOutputStream().flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void forwardToDestination() throws UnknownHostException, IOException, InterruptedException {
		System.out.println("Going to forward");
		byte[] headerByteArray = this.alterRequest.getHeader().getBytes(StandardCharsets.UTF_8);
		byte[] bodyByteArray = this.alterRequest.getBody().getBytes(StandardCharsets.UTF_8);
		Socket pCloudyAppiumHub = null;
		System.out.println("YOYO");
		if (this.serverEndpoint.getProtocol().equalsIgnoreCase("https")) {
			System.out.println("Cll: " + this.serverEndpoint.getHost());
			pCloudyAppiumHub = SSLSocketFactory.getDefault().createSocket(this.serverEndpoint.toString(), 0);
			SSLSocketFactory.getDefault().createSocket();
			System.out.println("Nope");
		} else {
			System.out.println("HTTP Forwarding: " + this.serverEndpoint.getPath());
			System.out.println("HOST: " + this.serverEndpoint.getHost());
			System.out.println("PORT: " + this.serverEndpoint.getPort());
			pCloudyAppiumHub = new Socket(this.serverEndpoint.getHost(), this.serverEndpoint.getPort());
		}
		System.out.println("Reach here");
		// Write Headers
		// ==================================================================================
		pCloudyAppiumHub.getOutputStream().write(headerByteArray);
		System.out.println("Socket PORT: " + pCloudyAppiumHub.getPort());
		System.out.println("Socket PORT: " + pCloudyAppiumHub.getInetAddress());
//		pCloudyAppiumHub.getOutputStream().write(headerByteArray);
		System.out.println("Now Reach here");
		// Write Body
		// ==================================================================================
		if(headerByteArray.toString().contains("POST") || headerByteArray.toString().contains("DELETE") ) {
			pCloudyAppiumHub.getOutputStream().write(bodyByteArray);
		}
		// Start Out Thread to Appium Hub
		// ================================================================================
		System.out.println("Going To read");
		try (AppiumToEclipseThread a2e = new AppiumToEclipseThread(pCloudyAppiumHub.getInputStream(), this.socketObject.getOutputStream(), this.proxy)) {
			Thread t2 = new Thread(a2e);
			t2.start();

			pCloudyAppiumHub.getOutputStream().flush();
			t2.join();

		}
	}

	public static void main(String[] args) {
		Map<String, String> map = new HashMap<String, String>();

		String value = "POST /session HTTP/1.1\n" + "User-Agent: selenium/3.141.59 (java unix)\n" + "Content-Type: application/json; charset=utf-8\n" + "Content-Length: 1097\n"
				+ "Host: 127.0.1.1:9400\n" + "Connection: Keep-Alive\n" + "Accept-Encoding: gzip";
		String[] lines = value.split("\n");

		map.put("type", lines[0].split(" ")[0]);
		map.put("endpoint", lines[0].split(" ")[1]);
		map.put("protocol", lines[0].split(" ")[2].split("/")[0]);

		for (int i = 1; i < lines.length; i++) {
			String[] split = lines[i].split(": ");
			map.put(split[0], split[1]);
		}

		for (String key : map.keySet()) {
			System.out.println(key + ": " + map.get(key));
		}
	}
}
