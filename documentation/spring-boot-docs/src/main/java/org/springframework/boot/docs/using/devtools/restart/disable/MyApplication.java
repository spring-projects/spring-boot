/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.using.devtools.restart.disable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {

	public static void main(String[] args) {
		System.setProperty("spring.devtools.restart.enabled", "false");
		SpringApplication.run(MyApplication.class, args);
	}

}
