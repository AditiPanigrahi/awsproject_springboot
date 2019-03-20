package com.cc.awsproject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

	@Value("${maxThreads}")
	int maxThreads;

	@Value("${QName}")
	String sqsQName;

	@Value("${S3Name}")
	String S3Name;

	@Value("${S3ResultName}")
	String S3ResultName;

	@Value("${S3Poll}")
	String S3Poll;

	@Value("${S3VideoName}")
	String S3VideoName;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		try {
			new ObjectDetectorController(maxThreads, sqsQName, S3Name, S3ResultName, S3Poll, S3VideoName).getObject();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
