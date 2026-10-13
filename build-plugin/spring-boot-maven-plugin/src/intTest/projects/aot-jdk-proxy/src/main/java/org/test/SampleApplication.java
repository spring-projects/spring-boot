/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import org.test.SampleApplication.SampleApplicationRuntimeHints;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.stereotype.Service;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(SampleApplicationRuntimeHints.class)
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}

	static class SampleApplicationRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			// Force creation of at least one JDK proxy
			hints.proxies().registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(Service.class));
		}
	}

}
