package com.cc.awsproject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.web.bind.annotation.RestController;
import com.amazonaws.util.EC2MetadataUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

@RestController
public class ObjectDetectorController {
	private int maxThreads;
	private int currentExecThreads = 0;
	private String QName;
	private String S3Name;
	private String S3ResultName;
	private final String queueURL;
	private Boolean terminate = false; // Set this variable to indicate shutdown has been requested
	private ExecutorService executorService;
	private List<Future<VideoResultKeyPair>> futures;
	private final String instanceId;
	
	private final AmazonSQS sqs;
	private final AmazonS3 s3;
	
	private final ReceiveMessageRequest rmr;

	public Boolean getTerminate() {
		return terminate;
	}

	public void setTerminate(Boolean terminate) {
		this.terminate = terminate;
	}

	public ObjectDetectorController(int maxThreads) {
		this.maxThreads = maxThreads;
		this.QName = GlobalConstants.AWS_SQS_NAME;
		this.S3Name = GlobalConstants.AWS_S3_COUNT_NAME;
		this.S3ResultName = GlobalConstants.AWS_S3_RESULT_NAME;
		this.executorService = Executors.newFixedThreadPool(this.maxThreads);
		this.futures = new ArrayList<Future<VideoResultKeyPair>>(this.maxThreads);
		for (int i = 0; i < maxThreads; i++)
			futures.add(null);

		this.sqs = AmazonSQSClientBuilder.defaultClient();
		this.queueURL = sqs.getQueueUrl(QName).getQueueUrl();
		this.rmr = new ReceiveMessageRequest(this.queueURL).withMaxNumberOfMessages(1);
		this.s3 = AmazonS3ClientBuilder.defaultClient();
		this.instanceId = EC2MetadataUtils.getInstanceId();
		
	}

	public Message getMsgFromQ() {

		List<Message> messages = this.sqs.receiveMessage(this.rmr).getMessages();
		return messages.get(0);

	}


	public void getObject() throws InterruptedException {

		while (true) {
			
			if(!terminate) {

			for (Future<VideoResultKeyPair> call : futures) {
				if (call == null) {
					Message msg = getMsgFromQ();
					
					if (msg != null) {
						call = executorService.submit(new ObjectDetectionModelService(new String(msg.getBody())));
						// Delete the message from queue after fetching and creating new Thread

						String messageReceiptHandle = msg.getReceiptHandle();
						sqs.deleteMessage(new DeleteMessageRequest(this.queueURL, messageReceiptHandle));

						currentExecThreads++;
						// Put Count into bucket
						try {
							s3.putObject(S3Name, this.instanceId, String.valueOf(currentExecThreads));
						} catch (AmazonS3Exception e) {
							e.printStackTrace();
						}						
					}
				} else if (call.isDone()) {
					try {
						VideoResultKeyPair kp = call.get();
		/*				new Thread(new putResultIntoS3(new String(kp.getVideoName()), new String(kp.getVideoResult())))
						.start();*/
						
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									s3.putObject(S3ResultName, new String(kp.getVideoName()), new String(kp.getVideoResult()));
								} catch (AmazonS3Exception e) {
									e.printStackTrace();
								}
							}
						}).start();

					} catch (InterruptedException | ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// Execution finished above
					call = null;
					currentExecThreads--;
					
					// Update value of this instance in s3 bucket
					
					try {
						s3.putObject(S3Name, this.instanceId, String.valueOf(currentExecThreads));
					} catch (AmazonS3Exception e) {
						e.printStackTrace();
					}
				}

			}
			}else {
				
				break;
			}
			
		}
		
		// Grace shutdown
		
		while(currentExecThreads != 0) {

			for (Future<VideoResultKeyPair> call : futures) {
				if(call != null && call.isDone()) {
					try {
						VideoResultKeyPair kp = call.get();
						new putResultIntoS3(new String(kp.getVideoName()), new String(kp.getVideoResult())).run();

					} catch (InterruptedException | ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					call = null;
					currentExecThreads--;
				}
			}
		}
		// Grace shutodwn call
		
		Thread.sleep(10000);

	}

}
