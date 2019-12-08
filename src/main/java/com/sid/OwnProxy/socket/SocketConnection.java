package com.sid.OwnProxy.socket;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;

import com.sid.OwnProxy.Global;
import com.sid.OwnProxy.interfaces.ProxySever;

public class SocketConnection {

	private static SocketConnection instance;	
	private URL proxyEndpoint;
	
	private SocketConnection() {
		// private
	}
	
	public static SocketConnection getInstance() {
		if(SocketConnection.instance == null) {
			SocketConnection.instance = new SocketConnection();
		}
		
		return SocketConnection.instance;
	}
	
	public URL intSocket(ProxySever userObject, URL destinstionURL) throws IOException, InterruptedException {
		if (this.proxyEndpoint == null) {
			this.setProxyEndpoint(destinstionURL);
			this.setSocket(userObject, destinstionURL);
		}

		return this.proxyEndpoint;
	}

	private void setProxyEndpoint(URL destinationURL) throws MalformedURLException, UnknownHostException {
		this.proxyEndpoint = new URL(Global.get().getProxyURL() + ":" + Global.get().getProxyPort() + destinationURL.getPath());
	}

	private void setSocket(ProxySever userObject, URL destinstionURL) throws IOException, InterruptedException {
		new SetSocket(userObject, destinstionURL).start();
		Thread.sleep(1000);
	}
}

class SetSocket extends Thread {

	ProxySever proxy;
	URL serverEndpoint;

	public SetSocket(ProxySever proxy, URL serverEndpoint) {
		this.proxy = proxy;
		this.serverEndpoint = serverEndpoint;
	}

	@Override
	public void run() {

		try {
			boolean listening = true;

			ServerSocket serverSocket = new ServerSocket(Global.get().getProxyPort());
			System.out.println("Proxy Server Started on " + Global.get().getProxyPort());
			System.out.println("Proxy Server Forwarded to " + this.serverEndpoint.toString());

			while (listening) {
				try {
					new ProxyThread(serverSocket.accept(), proxy, serverEndpoint).start();

				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}

			serverSocket.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
