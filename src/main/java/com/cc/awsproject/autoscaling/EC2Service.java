package com.cc.awsproject.autoscaling;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

@Service
public class EC2Service {

	@Autowired
	S3Service s3Service;

	private  Set<String> instantIdSet = new HashSet<>();
	
	private  Set<String> runningInstantIds = new HashSet<>();
	
	private  Queue<String> stoppedInstantIds = new LinkedList<>();

	public void createinstance() {
		
		final  AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		// need to change the ami image
		String imageId = "ami-0e355297545de2f82";  
		int minInstanceCount = 1; 
		int maxInstanceCount = 3;
		
		RunInstancesRequest rir = new RunInstancesRequest(imageId,
				minInstanceCount, maxInstanceCount);
		rir.setInstanceType("t2.micro"); 

		RunInstancesResult result = ec2.runInstances(rir);

		for(Instance instance : result.getReservation().getInstances()) {
			instantIdSet.add(instance.getInstanceId());
		}

	
		
	}
	public void createinstance(int maxInstanceCount) {

		if(instantIdSet.size() < 18) {
			final  AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

			// need to change the ami image
			String imageId = "ami-0e355297545de2f82";  
			int minInstanceCount = 1; 

			RunInstancesRequest rir = new RunInstancesRequest(imageId,
					minInstanceCount, maxInstanceCount);
			rir.setInstanceType("t2.micro"); 

			RunInstancesResult result = ec2.runInstances(rir);

			for(Instance instance : result.getReservation().getInstances()) {
				instantIdSet.add(instance.getInstanceId());
				runningInstantIds.add(instance.getInstanceId());
			}
		}

	}

	public  void startinstance(String instanceId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		StartInstancesRequest request = new StartInstancesRequest().
				withInstanceIds(instanceId);//start instance using the instance id
		ec2.startInstances(request);

	}

	public  void stopinstance(String instanceId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		StopInstancesRequest request = new StopInstancesRequest().
				withInstanceIds(instanceId);//stop instance using the instance id
		ec2.stopInstances(request);

	}

	public  void terminateinstance(String instanceId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		TerminateInstancesRequest request = new TerminateInstancesRequest().
				withInstanceIds(instanceId);//terminate instance using the instance id
		ec2.terminateInstances(request);

	}

	public void stopInstances() {

		if(runningInstantIds.size() > 1) {
			for(String instanceId : runningInstantIds) {
				int countOfliveRequests = s3Service.getBucketValue(instanceId,"bucket2");
				if(countOfliveRequests == 0) {
					stopinstance(instanceId);
					runningInstantIds.remove(instanceId);
					stoppedInstantIds.add(instanceId);
				}
			}
		}
		
	}

	public void terminateAll() {
		for(String instanceId : instantIdSet) {
			terminateinstance(instanceId);
		}
	}
	public  Set<String> getRunningInstanceIdSet(){
		
		return runningInstantIds;
	}
	public int getRunningInstanceCount() {
		return runningInstantIds.size();
	}
	public int getStoppedInstanceCount() {
		return stoppedInstantIds.size();
	}
	public void startinstance(int instaceCountNeedToStart) {
		
		for(int index = 0; index < instaceCountNeedToStart; index++) {
			String instanceId = stoppedInstantIds.poll();
			startinstance(instanceId);
			runningInstantIds.add(instanceId);
			stoppedInstantIds.remove(instanceId);
		}
		
	}


}

