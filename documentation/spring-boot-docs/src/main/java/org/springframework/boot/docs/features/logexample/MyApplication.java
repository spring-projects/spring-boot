/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.logexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample application used to collect logs for the reference doc.
 *
 * @author Stephane Nicoll
 */
@SpringBootApplication
public class MyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

}
