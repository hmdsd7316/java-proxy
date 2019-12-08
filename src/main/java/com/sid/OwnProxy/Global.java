package com.sid.OwnProxy;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import com.sid.OwnProxy.enums.Server;

public class Global {
	
	static class InstanceHelper{
		private static Global instance = new Global();
		public static Global getInstance() {
			return InstanceHelper.instance;
		}
	}
	
	public static Global get() {
		return InstanceHelper.getInstance();
	}
	
	public int getProxyPort() {
		return Server.PORT.getInteger();
	}
	
	public String getProxyURL() throws MalformedURLException, UnknownHostException {
		return Server.HOST.getHost();
	}
}
