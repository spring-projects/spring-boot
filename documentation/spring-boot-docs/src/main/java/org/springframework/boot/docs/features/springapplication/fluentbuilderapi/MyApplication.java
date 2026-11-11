/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.fluentbuilderapi;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class MyApplication {

	public void hierarchyWithDisabledBanner(String[] args) {
		// tag::code[]
		new SpringApplicationBuilder().sources(Parent.class)
			.child(Application.class)
			.bannerMode(Banner.Mode.OFF)
			.run(args);
		// end::code[]
	}

	static class Parent {

	}

	static class Application {

	}

}
