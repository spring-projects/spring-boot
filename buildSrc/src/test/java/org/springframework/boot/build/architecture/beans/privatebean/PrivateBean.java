/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.beans.privatebean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PrivateBean {

	@Bean
	static MyBean myBean() {
		return new MyBean();
	}

	private static final class MyBean {

	}

}
