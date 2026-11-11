/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loaderwarapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class LoaderWarTestApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(LoaderWarTestApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(LoaderWarTestApplication.class, args).close();
	}

}
