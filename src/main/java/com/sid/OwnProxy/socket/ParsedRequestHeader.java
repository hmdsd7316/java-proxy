package com.sid.OwnProxy.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sid.OwnProxy.enums.HttpProtocol;

public class ParsedRequestHeader {

	public HttpProtocol Protocol;
	public String address;
	public String httpVersion;

	public Map<String, String> RequestHeaders = new HashMap<>();

	private ParsedRequestHeader(String requestHeader) throws ParseException {
		// https://stackoverflow.com/questions/13255622/parsing-raw-http-request
		try {
			BufferedReader reader = new BufferedReader(new StringReader(requestHeader));
			String requestLine = reader.readLine();// Request-Line ; Section 5.1
			this.processRequestLine(requestLine);

			String header = reader.readLine();
			while (header.length() > 0) {
				appendHeaderParameter(header);
				header = reader.readLine();
			}

			reader.close();
		} catch (IOException e) {
			System.out.println("@Exception: While Parsing Request Header");
			// should never throw this exception
			// as we are reading from an String, not a Stream
		}

	}

	public static ParsedRequestHeader parse(String requestHeader) throws ParseException {
		return new ParsedRequestHeader(requestHeader);
	}

	private void processRequestLine(String requestLine) {
		String[] splits = requestLine.split(" ");
		this.Protocol = HttpProtocol.valueOf(splits[0]);
		this.address = splits[1];
		this.httpVersion = splits[2];
	}

	private void appendHeaderParameter(String header) throws ParseException {
		int idx = header.indexOf(":");
		if (idx == -1) {
			throw new ParseException("Invalid Header Parameter: " + header, 0);
		}
		RequestHeaders.put(header.substring(0, idx), header.substring(idx + 1, header.length()));
	}

	public String getSessionId() {
		Pattern p = Pattern.compile("(?<=\\/session\\/).*?(?=\\/)|(?<=\\/session\\/).*");
		Matcher m = p.matcher(this.address);
		if (m.find()) {
			return m.group();
		} else {
			return null;
		}

	}

}
