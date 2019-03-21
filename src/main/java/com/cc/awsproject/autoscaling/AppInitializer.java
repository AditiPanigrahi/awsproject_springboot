package com.cc.awsproject.autoscaling;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppInitializer {

	@Autowired
	S3Service s3Service;
	
	@Autowired
	Autoscaling autoscaling;
	
	@PostConstruct
	private void init() {
		
		//ec2Service.createinstance();
		
		// check if s3 bucket is there or not
		s3Service.createTheBucket("bucket2-akash");
		
		s3Service.createTheBucket("bucket3-akash");
		
		autoscaling.scaleOut(23);
		
		autoscaling.scaleIn(-5);
		
	}
}
