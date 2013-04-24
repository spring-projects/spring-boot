package org.springframework.bootstrap.sample.data;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class DataBootstrapApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DataBootstrapApplication.class, args);
	}

}
