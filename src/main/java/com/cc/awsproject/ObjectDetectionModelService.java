package com.cc.awsproject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.util.StreamUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;

public class ObjectDetectionModelService implements Callable<VideoResultKeyPair> {

	private String file = null;
	private String bucketName = null;

	private final static Object lock = new Object();

	public ObjectDetectionModelService(String filename, String bucketName) {
		this.file = filename;
		this.bucketName = bucketName;
	}

	// Downloads the video from s3 and return the path
	private String getVideopath() throws FileNotFoundException, IOException {

		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		String bucketName = this.bucketName;
		String key_Name = this.file;
		S3Object object = null;
		File file = null;
		System.out.println("Video Downloading started for " + key_Name);
		try {
			object = s3.getObject(bucketName, key_Name);
			InputStream reader = new BufferedInputStream(object.getObjectContent());
			file = new File(key_Name);
			OutputStream writer = new BufferedOutputStream(new FileOutputStream(file));
			int read = -1;
			while ((read = reader.read()) != -1) {
				writer.write(read);
			}
			writer.flush();
			writer.close();
			reader.close();
		} catch (AmazonS3Exception e) {
			e.printStackTrace();
		}
		return file.getAbsolutePath();
	}

	@Override
	public VideoResultKeyPair call() {
		String result = "No Object Detected";
		String AbsoluteFilePath = null;

		try {
			AbsoluteFilePath = getVideopath();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		synchronized (lock) {
			try {
				System.out.println("AbsoluteFilePath =" + AbsoluteFilePath + " Lock Acquired by " + Thread.currentThread().getId() );
				
				ProcessBuilder p0 = new ProcessBuilder(new String[]{"sh","-c","Xvfb :1 &"});
				Process process0 = p0.start();
				process0.waitFor();
				System.out.println("Process 0 result:- " + process0.exitValue());
				
				ProcessBuilder p1 = new ProcessBuilder("./darknet", "detector", "demo", "cfg/coco.data", "cfg/yolov3-tiny.cfg", "tiny.weights", AbsoluteFilePath, "-dont_show");
				Map<String,String> env = p1.environment();
				env.put("DISPLAY", ":1");
				p1.redirectOutput(new File("result"));
				Process process1 = p1.start();
				process1.waitFor();		

				System.out.println("Process 1 result:- " + process1.exitValue());
				
				Process process2 = Runtime.getRuntime().exec("./darknet_test.py");
				process2.waitFor();
				System.out.println("Process 2 result:- " + process2.exitValue());
				
				File f = new File("./result_label");
				BufferedInputStream bf = null ;
				if(f.exists()) {
					bf = new BufferedInputStream(new FileInputStream(f));
					result  = StreamUtils.copyToString(bf, StandardCharsets.UTF_8);
					System.out.println("Ouput : " + result);
				}
				
				process0.destroy();
				process1.destroy();
				process2.destroy();
	
				
				if(bf != null)
					bf.close();
				deleteVideoFile(AbsoluteFilePath);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
		}
		// read result from file and return
		// System.out.println(result);
		return new VideoResultKeyPair(file, result);

	}

	private void deleteVideoFile(String absoluteFilePath) {
		File file= new File(absoluteFilePath);
		if(file.delete()) 
		{ 
			System.out.println("File " + absoluteFilePath+ " deleted successfully"); 
		} 
		else
		{ 
			System.out.println("Failed to delete the file"); 
		} 

	}

}
