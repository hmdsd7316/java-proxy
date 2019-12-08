package com.sid.OwnProxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.sid.OwnProxy.interfaces.ProxySever;
import com.sid.OwnProxy.interfaces.Request;
import com.sid.OwnProxy.interfaces.Response;

/**
 * Hello world!
 *
 */
public class App implements ProxySever{
	
	static URL url;
	static URL endpoint;
    public static void main( String[] args ) throws IOException, InterruptedException
    {
        System.out.println( "Hello World!" );
        url = new URL("http://localhost:4723");
        endpoint = ProxyServices.getNewInstance().getProxyURL(new App(), url);
        
//        Socket pCloudyAppiumHub = new Socket("localhost", 4723);
//        pCloudyAppiumHub.getOutputStream().write(message.getBytes());
//        pCloudyAppiumHub.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
//        System.out.println("END");
    }

	@Override
	public Request request(Request request) {
		System.out.println("@Request===");
		System.out.println("Header: " + request.getHeader());
		System.out.println("body: " + request.getBody());
		System.out.println("====================================");
		
		String header = request.getHeader().replace("localhost:9400", url.getHost()+":"+ url.getPort());
		
		request.setHeader(header);
		request.setBody(request.getBody());
		
		System.out.println("After Header Change: " + request.getHeader());
		return request;
	}

	@Override
	public Response response(Response response) {
		System.out.println("@Response==");
		System.out.println("Header: " + response.getHeader());
		System.out.println("body: " + response.getBody());
		System.out.println("====================================");
		
		String body = "{" + 
				"  \"object\": {" + 
				"    \"a\": \"b\"," + 
				"    \"c\": \"d\"," + 
				"    \"e\": \"f\"" + 
				"  }," + 
				"  \"array\": [" + 
				"    1," + 
				"    2" + 
				"  ]," + 
				"  \"string\": \"Hello World\"" + 
				"}";
		
		response.setBody(body);
		return response;
	}
}

class SetSocket extends Thread {

	ProxySever proxy;
	URL serverEndpoint;

	public SetSocket(ProxySever proxy, URL serverEndpoint) {
		this.proxy = proxy;
		this.serverEndpoint = serverEndpoint;
	}
	
	String message = "POST /wd/hub/session HTTP/1.1" + 
			"User-Agent: selenium/3.141.59 (java windows)" + 
			"Content-Type: application/json; charset=utf-8" + 
			"Content-Length: 764" + 
			"Host: 192.168.0.8:9400" + 
			"Connection: Keep-Alive" + 
			"Accept-Encoding: gzip" + 
			"" + 
			"body: {" + 
			"  \"desiredCapabilities\": {" + 
			"    \"rotatable\": true," + 
			"    \"newCommandTimeout\": 600," + 
			"    \"noSign\": true," + 
			"    \"automationName\": \"uiautomator2\"," + 
			"    \"browserName\": \"chrome\"," + 
			"    \"appium:chromeOptions\": {" + 
			"      \"w3c\": false" + 
			"    }," + 
			"    \"platformName\": \"Android\"," + 
			"    \"deviceName\": \"PL2GAR4832302659\"," + 
			"    \"launchTimeout\": 90000" + 
			"  }," + 
			"  \"capabilities\": {" + 
			"    \"firstMatch\": [" + 
			"      {" + 
			"        \"appium:chromeOptions\": {" + 
			"          \"w3c\": false" + 
			"        }," + 
			"        \"appium:automationName\": \"uiautomator2\"," + 
			"        \"browserName\": \"chrome\"," + 
			"        \"appium:deviceName\": \"PL2GAR4832302659\"," + 
			"        \"appium:launchTimeout\": 90000," + 
			"        \"appium:newCommandTimeout\": 600," + 
			"        \"appium:noSign\": true," + 
			"        \"platformName\": \"android\"," + 
			"        \"rotatable\": true" + 
			"      }" + 
			"    ]" + 
			"  }" + 
			"}";

	@Override
	public void run() {

		
		
		try {
			boolean listening = true;

			ServerSocket serverSocket = new ServerSocket(4734);
			System.out.println("Proxy Server Started on " + 4734);
			System.out.println("Proxy Server Forwarded to " + this.serverEndpoint.toString());

			
					Socket socket = serverSocket.accept();
					socket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
					socket.getOutputStream().flush();
			serverSocket.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
