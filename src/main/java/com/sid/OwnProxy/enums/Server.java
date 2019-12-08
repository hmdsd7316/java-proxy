package com.sid.OwnProxy.enums;

import java.net.InetAddress;
import java.net.UnknownHostException;

public enum Server {

	HOST("http://"), PORT(9400), HUB("");
	
	private String value;
	private int number;
	
	private Server(String value) {
		this.value = value;
	}
	
	private Server(int value) {
		this.number = value;
	}

	public String getHost() throws UnknownHostException {
		return this.value + InetAddress.getLocalHost().getHostAddress();
	}
	
	public String getString() {
		return this.value;
	}

	public int getInteger() {
		return this.number;
	}


}
