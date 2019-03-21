package com.cc.awsproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AwsprojectApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwsprojectApplication.class, args);
	}
}
