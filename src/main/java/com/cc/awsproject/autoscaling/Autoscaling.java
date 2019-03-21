package com.cc.awsproject.autoscaling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Autoscaling {

	@Autowired
	SQSService sQSService;

	@Autowired
	S3Service s3Service;
	
	@Autowired
	EC2Service eC2Service;

	//@Scheduled(fixedDelay = 100000000)
	public void reportCurrentTime() {

		int countOfMessagesInVideoObjectRequestQueue = sQSService.getQMessageCount();
		int totalLiveRequestCount = s3Service.getTotalLiveRequestCount();
		
		int actualRequestNeedsToProcessed = countOfMessagesInVideoObjectRequestQueue - totalLiveRequestCount;
		
		if(actualRequestNeedsToProcessed > 0) {
			scaleOut(actualRequestNeedsToProcessed);
		}else {
			scaleIn(actualRequestNeedsToProcessed);
		}
		
	}
	
	
	//@Scheduled(fixedDelay = 20000)
	public void stopInstances() {
		eC2Service.stopInstances();
	}

	public void scaleIn(int actualRequestNeedsToProcessed) {
		
		int numberOfInstancesToStop = Math.abs((actualRequestNeedsToProcessed / 20));
		
		s3Service.updateBucket("bucket3",numberOfInstancesToStop);
		
	}

	public void scaleOut(int actualRequestNeedsToProcessed) {
		
		int numberOfNewInstancesRequired = (actualRequestNeedsToProcessed / 20) + 1;
		
		//int totalRunningInstances = eC2Service.getRunningInstanceCount();
		
		int totalStopInstances = eC2Service.getStoppedInstanceCount();
		
		int instanceCountNeedTocreate = numberOfNewInstancesRequired - totalStopInstances;
		
		if(instanceCountNeedTocreate > 0) {
			eC2Service.createinstance(instanceCountNeedTocreate);
		}
		
		int instaceCountNeedToStart = totalStopInstances - instanceCountNeedTocreate;
		
		if(instaceCountNeedToStart > 0) {
			eC2Service.startinstance(instaceCountNeedToStart);
		}
		
	}
}
