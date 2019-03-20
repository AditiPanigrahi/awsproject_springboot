package com.cc.awsproject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.util.StreamUtils;
import com.amazonaws.util.EC2MetadataUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class ObjectDetectorController {
	private int maxThreads;
	private int currentExecThreads = 0;
	private String QName;
	private String S3Name;
	private String S3ResultName;
	private String S3VideoName;
	private final String queueURL;
	private Boolean terminate = false; // Set this variable to indicate shutdown has been requested
	private ExecutorService executorService;
	private List<Future<VideoResultKeyPair>> futures;
	private final String instanceId;
	private final String S3Poll;
	private final AmazonSQS sqs;
	private final AmazonS3 s3;

	private final ReceiveMessageRequest rmr;

	public Boolean getTerminate() {
		return terminate;
	}

	public void setTerminate(Boolean terminate) {
		this.terminate = terminate;
	}

	public ObjectDetectorController(int maxThreads, String QName, String S3CountName, String S3ResultName,
			String S3Poll, String S3Video) {
		this.maxThreads = maxThreads;
		this.QName = QName;
		this.S3Name = S3CountName;
		this.S3ResultName = S3ResultName;
		this.executorService = Executors.newFixedThreadPool(this.maxThreads);
		this.futures = new ArrayList<Future<VideoResultKeyPair>>(this.maxThreads);
		for (int i = 0; i < maxThreads; i++)
			futures.add(null);

		this.sqs = AmazonSQSClientBuilder.defaultClient();
		this.queueURL = sqs.getQueueUrl(this.QName).getQueueUrl();
		this.rmr = new ReceiveMessageRequest(this.queueURL).withMaxNumberOfMessages(1);
		this.s3 = AmazonS3ClientBuilder.defaultClient();
		this.instanceId = EC2MetadataUtils.getInstanceId();
		this.S3Poll = S3Poll;
		this.S3VideoName = S3Video;
	}

	public Message getMsgFromQ() {

		List<Message> messages = this.sqs.receiveMessage(this.rmr).getMessages();
		if (messages.isEmpty())
			return null;
		return messages.get(0);

	}

	public boolean continuePoll() {

		S3Object toPoll = s3.getObject(this.S3Poll, "key");
		String doPoll = null;
		if (toPoll != null) {

			try {
				doPoll = StreamUtils.copyToString(toPoll.getObjectContent(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if ("true".equals(doPoll))
				return true;
			else
				return false;
		}
		return false;
	}

	public void getObject() throws InterruptedException {

		while (true) {

			if (continuePoll()) {

				for (int i = 0; i < futures.size(); i++) {
					Future<VideoResultKeyPair> call = futures.get(i);

					if (call == null) {
						Message msg = getMsgFromQ();

						if (msg != null) {
							futures.set(i, executorService
									.submit(new ObjectDetectionModelService(new String(msg.getBody()), S3VideoName)));
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
							System.out.println("Inside done");

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {
										s3.putObject(S3ResultName, new String(kp.getVideoName()),
												new String(kp.getVideoResult()));
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
						futures.set(i, null);
						currentExecThreads--;

						// Update value of this instance in s3 bucket

						try {
							s3.putObject(S3Name, this.instanceId, String.valueOf(currentExecThreads));
						} catch (AmazonS3Exception e) {
							e.printStackTrace();
						}
					}

				}
			} else {

				break;
			}

		}

		// Grace shutdown

		while (currentExecThreads != 0) {

			for (int i = 0; i < futures.size(); i++) {
				Future<VideoResultKeyPair> call = futures.get(i);
				{
					if (call != null && call.isDone()) {
						try {
							VideoResultKeyPair kp = call.get();

							try {
								s3.putObject(S3Name, new String(kp.getVideoName()), new String(kp.getVideoResult()));
							} catch (AmazonS3Exception e) {
								e.printStackTrace();
							}

						} catch (InterruptedException | ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						futures.set(i, null);
						currentExecThreads--;
					}
				}
			}
			// Grace shutdown call
		}
		Thread.sleep(5000);

	}

}
