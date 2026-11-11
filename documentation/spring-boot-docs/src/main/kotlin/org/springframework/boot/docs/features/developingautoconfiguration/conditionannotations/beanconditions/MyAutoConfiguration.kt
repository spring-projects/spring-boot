/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.conditionannotations.beanconditions

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class MyAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	fun someService(): SomeService {
		return SomeService()
	}

}

