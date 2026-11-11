/*
 * Copyright 2012-present the original author or authors.
 */

package com.example;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerPortFileWriter;

@SpringBootApplication
class DevToolsTestApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(DevToolsTestApplication.class).listeners(new WebServerPortFileWriter(args[0]))
			.run(args);
	}

}
