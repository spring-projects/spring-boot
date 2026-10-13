/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.resttemplate.customization;

import java.time.Duration;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.autoconfigure.RestTemplateBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyRestTemplateBuilderConfiguration {

	@Bean
	public RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {
		return configurer.configure(new RestTemplateBuilder())
			.connectTimeout(Duration.ofSeconds(5))
			.readTimeout(Duration.ofSeconds(2));
	}

}
