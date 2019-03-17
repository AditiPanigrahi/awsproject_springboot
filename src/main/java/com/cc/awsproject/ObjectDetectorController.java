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

@RestController
public class ObjectDetectorController {
	@RequestMapping("/")
	public String getObject() {
		String object = null;
		VideoService videoService = new VideoService();
		String videoFile= videoService.getVideo();
		ObjectDetectionModelService detectObject = new ObjectDetectionModelService();
		object = detectObject.findObjectfromVideo(videoFile);
		System.out.println("Object detection complete");
		return object;
	}

}
