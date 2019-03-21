package com.cc.awsproject.autoscaling;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppTerminator {

	@Autowired
	EC2Service ec2Service;
	
	@PreDestroy
	public void terminate() {
		
		ec2Service.terminateAll();
	}
}
