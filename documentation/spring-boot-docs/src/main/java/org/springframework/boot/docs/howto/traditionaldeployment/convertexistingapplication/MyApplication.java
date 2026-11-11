/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.traditionaldeployment.convertexistingapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class MyApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		// Customize the application or call application.sources(...) to add sources
		// Since our example is itself a @Configuration class (through
		// @SpringBootApplication)
		// we actually do not need to override this method.
		return application;
	}

	// tag::main[]
	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}
	// end::main[]

}
