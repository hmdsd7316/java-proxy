package com.sid.proxy.sockets;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyMain {

	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = null;
		boolean listening = true;
		int port = 1234;
		String defaultEndpoint = "http://0.0.0.0:4723";
		if (args.length == 0) {
			System.out.println("Proxy is running on " + port + " and Endpoint is " + defaultEndpoint);
		} else if (args.length == 1) {
			if (args[0].length() == 4) {
				port = Integer.parseInt(args[0]);
				System.out.println("Proxy is running on " + port + " and Endpoint is " + defaultEndpoint);
			} else {
				defaultEndpoint = args[0];
				System.out.println("Proxy is running on " + port + " and Endpoint is " + defaultEndpoint);
			}
		}

		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Port Started on " + port);
		} catch (IOException e) {
			System.out.println("Port Error");
			System.exit(-1);
		}

		while (listening) {
			new ProxyThread(serverSocket.accept(), defaultEndpoint).start();
		}
		serverSocket.close();
	}
}

class ProxyThread extends Thread {

	private Socket eclipseSocket = null;
	public String defaultEndpoint = null;
	public static final int BUFFER_SIZE = 1024;

	public ProxyThread(Socket eclipseSocket, String defaultEndpoint) {
		this.eclipseSocket = eclipseSocket;
		this.defaultEndpoint = defaultEndpoint;

	}

	public void run() {
		System.out.println("=================== New Thread started ===============" + Thread.currentThread().getName());
		try {

			// Lets read from Eclipse Socket and write the request to Appium Socket
			ProxyToServer e2a = new ProxyToServer(eclipseSocket, defaultEndpoint);
			Thread t1 = new Thread(e2a);
			t1.start();

			t1.join();

		} catch (InterruptedException ex) {
			Logger.getLogger(ProxyThread.class.getName()).log(Level.SEVERE, null, ex);

		} finally {
			// try {
			// System.out.println("Closing sockets");
			// //eclipseSocket.close();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
		}
	}

}
