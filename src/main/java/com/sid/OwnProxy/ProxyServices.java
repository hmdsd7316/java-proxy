package com.sid.OwnProxy;

import java.io.IOException;
import java.net.URL;

import com.sid.OwnProxy.interfaces.ProxySever;
import com.sid.OwnProxy.socket.SocketConnection;

public class ProxyServices {
	
	
	private ProxyServices() {
		
	}
	
	public static ProxyServices getNewInstance() {
		return new ProxyServices();
	}
	
	public URL getProxyURL(ProxySever userObject, URL url) throws IOException, InterruptedException {
		return SocketConnection.getInstance().intSocket(userObject, url);
	}
}
