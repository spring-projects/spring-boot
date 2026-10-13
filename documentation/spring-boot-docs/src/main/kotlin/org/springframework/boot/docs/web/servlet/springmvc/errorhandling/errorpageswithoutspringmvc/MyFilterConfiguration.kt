/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.springmvc.errorhandling.errorpageswithoutspringmvc

import jakarta.servlet.DispatcherType
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.EnumSet

@Configuration(proxyBeanMethods = false)
class MyFilterConfiguration {

	@Bean
	fun myFilter(): FilterRegistrationBean<MyFilter> {
		val registration = FilterRegistrationBean(MyFilter())
		// ...
		registration.setDispatcherTypes(EnumSet.allOf(DispatcherType::class.java))
		return registration
	}

}

