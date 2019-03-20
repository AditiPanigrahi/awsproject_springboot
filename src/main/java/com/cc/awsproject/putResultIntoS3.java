package com.cc.awsproject;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;

public class putResultIntoS3 implements Runnable {

	private String result;
	private String Video;
	final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
	public putResultIntoS3(String Video, String result) {
		this.result = result;
		this.Video = Video;
	}

	@Override
	public void run() {
		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		String bucketName = "piyushbucket123456";

		try {
			s3.putObject(bucketName, this.Video, this.result);
		} catch (AmazonS3Exception e) {
			e.printStackTrace();
		}

	}
}
