/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.thirdpartyconfiguration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ThirdPartyConfiguration {

	@Bean
	@ConfigurationProperties("another")
	fun anotherComponent(): AnotherComponent = AnotherComponent()

}

