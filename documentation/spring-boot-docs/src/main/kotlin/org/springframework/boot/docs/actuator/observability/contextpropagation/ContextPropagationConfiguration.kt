/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.observability.contextpropagation

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.support.ContextPropagatingTaskDecorator

@Configuration(proxyBeanMethods = false)
class ContextPropagationConfiguration {

	@Bean
	fun contextPropagatingTaskDecorator(): ContextPropagatingTaskDecorator {
		return ContextPropagatingTaskDecorator()
	}

}
