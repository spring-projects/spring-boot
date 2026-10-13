/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.applicationexit;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MyApplication {

	@Bean
	public ExitCodeGenerator exitCodeGenerator() {
		return () -> 42;
	}

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(MyApplication.class, args)));
	}

}
