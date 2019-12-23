package com.sid.proxy.alter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.sid.proxy.interfaces.ProxySever;
import com.sid.proxy.interfaces.Request;
import com.sid.proxy.interfaces.Response;
import com.sid.proxy.interfaces.ServerResponse;

public class AlterRR implements ProxySever{
	
	boolean isScreenshot;
	
	@Override
	public Request request(Request request) {
		System.out.println("@Request===");
		System.out.println("Header: " + request.getHeader());
		System.out.println("body: " + request.getBody());
		System.out.println("====================================");
		
		String header = request.getHeader();
		
		if (header.contains("/screenshot")) {
			this.isScreenshot = true;
		}
				
		return request;
	}

	@Override
	public Response response(Response response) throws IOException, Exception {
		System.out.println("@Response==");
		System.out.println("Header: " + response.getHeader());
		System.out.println("body: " + response.getBody());
		System.out.println("====================================");
		
		if(isScreenshot) {
			this.isScreenshot = false;
			//return returnScreenshotResponse(convertImageToBase64(takeSnapShotViaADB()));
		}
		
		return response;
	}
	
	private Response returnScreenshotResponse(String encodedString) {

		System.out.println("Sending Hub Status to Eclipse");
		DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));

		String header = "HTTP/1.1 200 OK\n" + "X-Powered-By: Express\n" + "access-control-allow-origin: *\n" + "access-control-allow-methods: GET,POST,PUT,OPTIONS,DELETE\n"
				+ "access-control-allow-headers: origin, content-type, accept\n" + "vary: X-HTTP-Method-Override\n" + "content-type: application/json; charset=utf-8\n" + "content-length: #length#\n"
				+ "ETag: W/\"4c-ls0DQ39q6iMpa8keP7gdpBWjP20\"" + "\n" + "Date: " + df.format(new Date()) + "\n" + "connection: close" + "\n";

		org.json.JSONObject _jsonBody = new JSONObject();
		_jsonBody.put("value", encodedString);
		String body = _jsonBody.toString(); // "{\"value\":\"" + encodedString + "\"}";

		header = header.replace("#length#", body.length() + "");

		Response response = new ServerResponse();
		response.setHeader(header);
		response.setBody(body);
		
		System.out.println("Screenshot: Header: " + response.getHeader());
		System.out.println("Screenshot: Body: " + response.getBody());
		
		return response;
		
	}
	
	public static void main(String[] args) throws Exception {
		new AlterRR().takeSnapShotViaADB();
	}
	
	
	public File takeSnapShotViaADB() throws Exception {
		File ImageFile = File.createTempFile("Step", ".png");
		String IMG_NAME = "";

		IMG_NAME = "Img" + System.currentTimeMillis() + ".png";

		if (ImageFile == null) {
			ImageFile = new File(System.getProperty("user.dir"), "Image");
		}

		System.out.println("Capture Snapshot ::: " + "Image Name " + IMG_NAME + " Desired Image File " + ImageFile.getAbsolutePath());
		
		if (ImageFile.getParentFile().exists() == false)
			ImageFile.getParentFile().mkdirs();

		String CREATE_IMAGE = "adb -s PL2GAR4832302659 shell screencap /sdcard/"+ IMG_NAME;
		String PULL_COMMAND = "adb -s PL2GAR4832302659 pull /sdcard/" + IMG_NAME + " " + ImageFile.getAbsolutePath();
		String RM_IMAGE_COMMAND = "adb -s PL2GAR4832302659  shell rm -r /sdcard/" + IMG_NAME;
		
		System.out.println(CREATE_IMAGE);
		System.out.println(PULL_COMMAND);
		System.out.println(RM_IMAGE_COMMAND);
		
		runCommand(CREATE_IMAGE);
		runCommand(PULL_COMMAND);
		runCommand(RM_IMAGE_COMMAND);
		System.out.println("Image Captured");
		return ImageFile;
	}
	
	public static String runCommand(String command) throws InterruptedException, IOException {
		Process p = Runtime.getRuntime().exec(command);
		// get std output
		BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = "";
		String allLine = "";
		while ((line = r.readLine()) != null) {
			allLine = allLine + "" + line + "\n";
			if (line.contains("Console LogLevel: debug") && line.contains("Complete")) {
				break;
			}
		}
		return allLine;
	}
	
	private String convertImageToBase64(File file) throws IOException {
		byte[] fileContent = FileUtils.readFileToByteArray(file);
		String encodedString = Base64.getEncoder().encodeToString(fileContent);
		return encodedString;
	}

}
