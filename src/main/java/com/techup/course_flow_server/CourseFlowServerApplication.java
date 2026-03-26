package com.techup.course_flow_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CourseFlowServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CourseFlowServerApplication.class, args);
	}

}
