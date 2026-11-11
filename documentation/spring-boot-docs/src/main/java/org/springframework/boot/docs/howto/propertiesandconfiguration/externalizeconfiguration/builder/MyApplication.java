/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.propertiesandconfiguration.externalizeconfiguration.builder;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class MyApplication {

	public static void main(String[] args) {
		// @formatter:off
		new SpringApplicationBuilder()
			.bannerMode(Banner.Mode.OFF)
			.sources(MyApplication.class)
			.run(args);
		// @formatter:on
	}

}
