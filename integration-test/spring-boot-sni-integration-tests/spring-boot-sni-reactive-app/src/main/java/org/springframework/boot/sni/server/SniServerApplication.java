/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sni.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SniServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SniServerApplication.class, args);
	}

}
