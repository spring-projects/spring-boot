/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.resttemplate.customization

import org.springframework.boot.restclient.autoconfigure.RestTemplateBuilderConfigurer
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class MyRestTemplateBuilderConfiguration {

	@Bean
	fun restTemplateBuilder(configurer: RestTemplateBuilderConfigurer): RestTemplateBuilder {
		return configurer.configure(RestTemplateBuilder()).connectTimeout(Duration.ofSeconds(5))
			.readTimeout(Duration.ofSeconds(2))
	}

}

