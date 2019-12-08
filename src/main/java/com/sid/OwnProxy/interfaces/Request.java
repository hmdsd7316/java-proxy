package com.sid.OwnProxy.interfaces;

public interface Request {
	
	public String getHeader();
	
	public String getBody();
	
	public void setHeader(String header);
	
	public void setBody(String body);
}
