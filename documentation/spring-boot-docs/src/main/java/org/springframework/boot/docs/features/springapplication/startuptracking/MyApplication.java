/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.startuptracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
public class MyApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(MyApplication.class);
		application.setApplicationStartup(new BufferingApplicationStartup(2048));
		application.run(args);
	}

}
