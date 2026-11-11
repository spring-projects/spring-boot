/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.testing

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.docs.features.developingautoconfiguration.testing.MyServiceAutoConfiguration.UserProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MyService::class)
@EnableConfigurationProperties(
	UserProperties::class
)
class MyServiceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	fun userService(properties: UserProperties): MyService {
		return MyService(properties.name)
	}

	@ConfigurationProperties("user")
	class UserProperties {
		var name = "test"
	}

}

