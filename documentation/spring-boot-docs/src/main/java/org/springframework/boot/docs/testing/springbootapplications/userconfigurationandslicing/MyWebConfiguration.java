/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.userconfigurationandslicing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class MyWebConfiguration {

	@Bean
	public WebMvcConfigurer testConfigurer() {
		return new WebMvcConfigurer() {
			// ...
		};
	}

}
