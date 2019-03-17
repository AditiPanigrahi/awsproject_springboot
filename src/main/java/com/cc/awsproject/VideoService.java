package com.cc.awsproject;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


public class VideoService {

	private static final String FILENAME = "filename=";
	private static final String FILE_NAME_FROM_HEADER = "Content-Disposition";
	private static final String VIDEO_URL= "http://206.207.50.7/getvideo";
	public String getVideo() {
		String fileName = null;
		try {
			URL url = new URL(VIDEO_URL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(HttpMethod.GET.toString());
			InputStream inputStream = con.getInputStream();
			Integer responseCode = con.getResponseCode();
				
			if(responseCode == HttpURLConnection.HTTP_OK) {
				fileName = processConnection(con, fileName);
			}
			FileOutputStream outputStream = new FileOutputStream(fileName);
            writeToFile(inputStream, outputStream);
            outputStream.close();
            inputStream.close();
            System.out.println(fileName);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
		}

		return fileName;
	}

	private void writeToFile(InputStream inputStream, FileOutputStream outputStream) throws IOException {
		int bytesRead = -1;
		byte[] buffer = new byte[4096];
		while ((bytesRead = inputStream.read(buffer)) != -1) {
		    outputStream.write(buffer, 0, bytesRead);
		}
	}

	private String processConnection(HttpURLConnection con, String fileName) {
		String disposition = con.getHeaderField(FILE_NAME_FROM_HEADER);
		/*String contentType = con.getContentType();
		int contentLength = con.getContentLength();*/
		if (disposition!=null) {
			fileName = getFilename(fileName, disposition);
		}
		return fileName;
	}

	private String getFilename(String fileName, String disposition) {
		int index = disposition.indexOf(FILENAME);
		if (index > 0) {
		    fileName = disposition.substring(index + 11,
		            disposition.length() - 1);
		}
		return fileName;
	}	

}
