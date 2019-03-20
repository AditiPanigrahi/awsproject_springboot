package com.cc.awsproject;

public class VideoResultKeyPair {

	public VideoResultKeyPair(String videoName, String videoResult) {
		super();
		VideoName = videoName;
		VideoResult = videoResult;
	}
	private String VideoName;
	private String VideoResult;
	
	public String getVideoName() {
		return VideoName;
	}
	public String getVideoResult() {
		return VideoResult;
	}
	
}
