package com.cc.awsproject.autoscaling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

@Service
public class S3Service {

	@Autowired
	EC2Service ec2Service;
	
	public int  getTotalLiveRequestCount() {
		
		int totalLiveRequestCount = 0;
		final AmazonS3  s3 = AmazonS3ClientBuilder.defaultClient();
		String bucketName = "myfirstbucketakash1";
		
		Set<String> instanceIds = ec2Service.getRunningInstanceIdSet();
		
		for(String instanceId : instanceIds) {
			try {
				
				
				S3Object object = s3.getObject(new GetObjectRequest(bucketName,instanceId));
				S3ObjectInputStream inputStream = object.getObjectContent();
				byte[] objectByteArray = IOUtils.toByteArray(inputStream);
				
				 String liveRequestCount =  deserialize(objectByteArray);
				 
				 totalLiveRequestCount += Integer.parseInt(liveRequestCount);
				 
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
		
		return instanceIds.size()*20 - totalLiveRequestCount;
	}
	
	public String deserialize(byte[] data) {
	    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
	    ObjectInputStream objectInputStream;
	    String liveRequestCount = null;
		try {
			objectInputStream = new ObjectInputStream(byteArrayInputStream);
			liveRequestCount = (String) objectInputStream.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return liveRequestCount;
	}
	
	public void putObjectInBucket(String bucketName,String instanceId) {
		final AmazonS3  s3 = AmazonS3ClientBuilder.defaultClient();
		
		
			try {
				
				 s3.putObject(bucketName,instanceId,"false");
				
			} catch (AmazonServiceException  e) {
				System.err.println(e.getMessage());
			}
		
		
	}
	
	private byte[] serialize(Object obj)  {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os;
		try {
			os = new ObjectOutputStream(out);
			os.writeObject(obj);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return out.toByteArray();
	}

	public Set<String> getminliveRequestCountInstances(int numberOfInstancesToterminate, Set<String> runningInstantIdSet) {
		
		Set<String> minliveRequestCountInstances = new HashSet<>();
		
		String[][] countAndInstanceArray = new String[runningInstantIdSet.size()][2]; 
		final AmazonS3  s3 = AmazonS3ClientBuilder.defaultClient();
		String bucketName = "myfirstbucketakash1";
		int index = 0;
		for(String instanceId : runningInstantIdSet) {
			try {
				
				
				S3Object object = s3.getObject(new GetObjectRequest(bucketName,instanceId));
				S3ObjectInputStream inputStream = object.getObjectContent();
				byte[] x = IOUtils.toByteArray(inputStream);
				
				 String liveRequestCount = (String) deserialize(x);
				 countAndInstanceArray[index][0] = liveRequestCount;
				 countAndInstanceArray[index][1] = instanceId;
			} catch (Exception e) {
				// TODO: handle exception
			}
			index++;
		}
		
		Arrays.sort(countAndInstanceArray, (a, b) -> Integer.compare(Integer.parseInt(a[0]), Integer.parseInt(b[0])));
		
		for (int i = 0; i < numberOfInstancesToterminate; i++) {
			minliveRequestCountInstances.add(countAndInstanceArray[i][1]);
		}
		return minliveRequestCountInstances;
	}

	public void updateBucket(String bucketName, int numberOfInstancesToterminate) {
		
		Set<String> runningInstantIdSet = ec2Service.getRunningInstanceIdSet();
		Set<String> minliveRequestCountInstances = getminliveRequestCountInstances(numberOfInstancesToterminate,runningInstantIdSet);
		
		for (String instanceId : minliveRequestCountInstances) {
			putObjectInBucket(bucketName,instanceId);
		}
		
	
	}

	public int getBucketValue(String instanceId,String bucketName) {

		
		String liveRequestCount = "0";
		final AmazonS3  s3 = AmazonS3ClientBuilder.defaultClient();
		
			try {
				
				
				S3Object object = s3.getObject(new GetObjectRequest(bucketName,instanceId));
				S3ObjectInputStream inputStream = object.getObjectContent();
				byte[] x = IOUtils.toByteArray(inputStream);
				
				 liveRequestCount = (String) deserialize(x);
				
				 
			} catch (Exception e) {
				// TODO: handle exception
			}
		
		
		
		return Integer.parseInt(liveRequestCount);
	
		
	}

	public void createTheBucket(String bucketName) {
		final AmazonS3  s3 = AmazonS3ClientBuilder.defaultClient();
		try {
			s3.createBucket(bucketName);
		} catch (AmazonS3Exception e) {
			System.out.println(e.getMessage());
		}
		
	}
}
