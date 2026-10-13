/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.beans.regular;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RegularBean {

	@Bean
	static PackagePrivate packagePrivateBean() {
		return new PackagePrivate();
	}

	@Bean
	static Protected protectedBean() {
		return new Protected();
	}

	@Bean
	static Public publicBean() {
		return new Public();
	}

	static final class PackagePrivate {

	}

	protected static final class Protected {

	}

	public static final class Public {

	}

}
