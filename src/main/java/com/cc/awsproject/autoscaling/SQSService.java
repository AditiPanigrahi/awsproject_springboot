package com.cc.awsproject.autoscaling;

import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;

@Service
public class SQSService{
	private static final String QUEUE_NAME = "VideoObjectRequestQueue";


	public  int  getQMessageCount() {
		final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
		GetQueueAttributesRequest getQueueAttributesRequest 
		= new GetQueueAttributesRequest(queueUrl)
		.withAttributeNames("All");

		int  ApproximateNumberOfMessages  = Integer.parseInt(sqs.getQueueAttributes(getQueueAttributesRequest)
				.getAttributes().get("ApproximateNumberOfMessages") != null ? sqs.getQueueAttributes(getQueueAttributesRequest)
						.getAttributes().get("ApproximateNumberOfMessages") : "0");
		int ApproximateNumberOfMessagesNotVisible  = Integer.parseInt(sqs.getQueueAttributes(getQueueAttributesRequest)
				.getAttributes().get("ApproximateNumberOfMessagesNotVisible") != null ? sqs.getQueueAttributes(getQueueAttributesRequest)
						.getAttributes().get("ApproximateNumberOfMessagesNotVisible") : "0");
		int ApproximateNumberOfMessagesDelayed   = Integer.parseInt(sqs.getQueueAttributes(getQueueAttributesRequest)
				.getAttributes().get("ApproximateNumberOfMessagesDelayed ") != null ? sqs.getQueueAttributes(getQueueAttributesRequest)
						.getAttributes().get("ApproximateNumberOfMessagesDelayed ") : "0");

		return ApproximateNumberOfMessages+ApproximateNumberOfMessagesNotVisible+ApproximateNumberOfMessagesDelayed;
	}

}
