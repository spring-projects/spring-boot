/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.traditionaldeployment.convertexistingapplication.both;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class MyApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return customizerBuilder(builder);
	}

	public static void main(String[] args) {
		customizerBuilder(new SpringApplicationBuilder()).run(args);
	}

	private static SpringApplicationBuilder customizerBuilder(SpringApplicationBuilder builder) {
		return builder.sources(MyApplication.class).bannerMode(Banner.Mode.OFF);
	}

}
