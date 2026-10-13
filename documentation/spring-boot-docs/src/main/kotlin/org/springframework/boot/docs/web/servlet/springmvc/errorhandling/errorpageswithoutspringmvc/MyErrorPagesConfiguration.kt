/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.springmvc.errorhandling.errorpageswithoutspringmvc

import org.springframework.boot.web.error.ErrorPage
import org.springframework.boot.web.error.ErrorPageRegistrar
import org.springframework.boot.web.error.ErrorPageRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus

@Configuration(proxyBeanMethods = false)
class MyErrorPagesConfiguration {

	@Bean
	fun errorPageRegistrar(): ErrorPageRegistrar {
		return ErrorPageRegistrar { registry: ErrorPageRegistry -> registerErrorPages(registry) }
	}

	private fun registerErrorPages(registry: ErrorPageRegistry) {
		registry.addErrorPages(ErrorPage(HttpStatus.BAD_REQUEST, "/400"))
	}

}

