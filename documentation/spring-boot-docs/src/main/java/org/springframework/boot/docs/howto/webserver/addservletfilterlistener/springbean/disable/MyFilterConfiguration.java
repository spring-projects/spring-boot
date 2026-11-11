/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.addservletfilterlistener.springbean.disable;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyFilterConfiguration {

	@Bean
	public FilterRegistrationBean<MyFilter> registration(MyFilter filter) {
		FilterRegistrationBean<MyFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

}
