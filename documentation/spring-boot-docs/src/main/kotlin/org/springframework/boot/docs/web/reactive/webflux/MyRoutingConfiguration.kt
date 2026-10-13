/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.webflux

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.DELETE
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration(proxyBeanMethods = false)
class MyRoutingConfiguration {

	@Bean
	fun monoRouterFunction(userHandler: MyUserHandler): RouterFunction<ServerResponse> {
		return RouterFunctions.route(
			GET("/{user}").and(ACCEPT_JSON), userHandler::getUser).andRoute(
			GET("/{user}/customers").and(ACCEPT_JSON), userHandler::getUserCustomers).andRoute(
			DELETE("/{user}").and(ACCEPT_JSON), userHandler::deleteUser)
	}

	companion object {
		private val ACCEPT_JSON = accept(MediaType.APPLICATION_JSON)
	}

}

