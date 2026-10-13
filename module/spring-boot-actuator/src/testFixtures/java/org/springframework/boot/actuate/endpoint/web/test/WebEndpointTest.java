/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTestInvocationContextProvider.WebEndpointsInvocationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Signals that a test should be run against one or more of the web endpoint
 * infrastructure implementations (Jersey, Web MVC, and WebFlux).
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@TestTemplate
@ExtendWith(WebEndpointTestInvocationContextProvider.class)
public @interface WebEndpointTest {

	/**
	 * The infrastructure against which the test should run.
	 * @return the infrastructure to run the tests against
	 */
	Infrastructure[] infrastructure() default { Infrastructure.JERSEY, Infrastructure.MVC, Infrastructure.WEBFLUX };

	enum Infrastructure {

		/**
		 * Actuator running on the Jersey-based infrastructure.
		 */
		JERSEY("Jersey"),

		/**
		 * Actuator running on the WebMVC-based infrastructure.
		 */
		MVC("WebMvc"),

		/**
		 * Actuator running on the WebFlux-based infrastructure.
		 */
		WEBFLUX("WebFlux");

		private final String name;

		Infrastructure(String name) {
			this.name = name;
		}

		WebEndpointsInvocationContext createInvocationContext(
				Function<List<Class<?>>, ConfigurableApplicationContext> contextFactory) {
			return new WebEndpointsInvocationContext(this.name, contextFactory);
		}

	}

}
