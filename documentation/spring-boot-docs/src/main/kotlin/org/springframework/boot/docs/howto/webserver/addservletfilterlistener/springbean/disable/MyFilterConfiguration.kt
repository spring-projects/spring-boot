/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.addservletfilterlistener.springbean.disable

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyFilterConfiguration {

	@Bean
	fun registration(filter: MyFilter): FilterRegistrationBean<MyFilter> {
		val registration = FilterRegistrationBean(filter)
		registration.isEnabled = false
		return registration
	}

}

