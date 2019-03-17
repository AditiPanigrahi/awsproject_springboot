package com.cc.awsproject;

import java.io.File;
import java.io.IOException;

public class ObjectDetectionModelService {

	private static final String NO_OBJECT_FOUND = "No Object Found";

	public String findObjectfromVideo(String file) {
		String object=invokeModel(file);
		if(object!=null) {
			return object;
		}
		else{
			return NO_OBJECT_FOUND;
		}
	}

	public String invokeModel(String file) {
		//invoke deep learning model form a script and parse it inside script using .py script
		String result=null;
		//File dir = new File("~/darknet/");
		//String[] cmdArray = new String[2];
		//cmdArray[0]="invokeModel.sh";
		//cmdArray[1]=file;
		try {
			System.out.println("Inside object detection class");
			Process process1 = Runtime.getRuntime().exec("./darknet detector demo cfg/coco.data cfg/yolov3-tiny.cfg  tiny.weights "+file+" -dont_show > result");
			process1.waitFor();
			Process process2 = Runtime.getRuntime().exec("./darknet_test.py");
			process2.waitFor();
			result=process2.getOutputStream().toString();
			System.out.println(result);
			
			//process = Runtime.getRuntime().exec("rm -rf"+file, null, dir);
			process1.destroy();
			process2.destroy();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());	
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//read result from file and return
		return result;
	}


}
