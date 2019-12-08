package com.sid.OwnProxy.interfaces;

public interface ProxySever {

	public Request request(Request request);
	
	public Response response(Response response);
}