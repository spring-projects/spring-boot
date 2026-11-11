/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.packaging.nativeimage.introducinggraalvmnativeimages.understandingaotprocessing.sourcecodegeneration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyConfiguration {

	@Bean
	public MyBean myBean() {
		return new MyBean();
	}

}
