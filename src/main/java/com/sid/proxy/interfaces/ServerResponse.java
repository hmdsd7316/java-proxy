package com.sid.proxy.interfaces;

public class ServerResponse implements Response{

	private String header;
	private String body;
	
	@Override
	public String getHeader() {
		return this.header;
	}

	@Override
	public String getBody() {
		return this.body;
	}

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public void setBody(String body) {
		this.body = body;
	}
	
}
