package com.sid.proxy.interfaces;

import java.io.IOException;

public interface ProxySever {

	public Request request(Request request);
	
	public Response response(Response response) throws IOException, Exception;
}