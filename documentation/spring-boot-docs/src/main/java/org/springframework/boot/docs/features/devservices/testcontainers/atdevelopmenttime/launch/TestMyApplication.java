/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.launch;

import org.springframework.boot.SpringApplication;

public class TestMyApplication {

	public static void main(String[] args) {
		SpringApplication.from(MyApplication::main).run(args);
	}

}
