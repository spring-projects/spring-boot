/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.testing;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.docs.features.developingautoconfiguration.testing.MyServiceAutoConfiguration.UserProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(UserProperties.class)
public final class MyServiceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	MyService userService(UserProperties properties) {
		return new MyService(properties.getName());
	}

	@ConfigurationProperties("user")
	public static class UserProperties {

		private String name = "test";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
