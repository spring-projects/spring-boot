/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.conditionannotations.classconditions

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
// Some conditions ...
class MyAutoConfiguration {

	// Auto-configured beans ...
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SomeService::class)
	class SomeServiceConfiguration {

		@Bean
		@ConditionalOnMissingBean
		fun someService(): SomeService {
			return SomeService()
		}

	}

}

