/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.using.structuringyourcode.locatingthemainclass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

}
