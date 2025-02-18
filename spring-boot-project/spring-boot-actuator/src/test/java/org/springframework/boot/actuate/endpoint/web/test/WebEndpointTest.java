/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * infrastructure implementations (Jersey, Web MVC, and WebFlux)
 *
 * @author Andy Wilkinson
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
		JERSEY("Jersey", WebEndpointTestInvocationContextProvider::createJerseyContext),

		/**
		 * Actuator running on the WebMVC-based infrastructure.
		 */
		MVC("WebMvc", WebEndpointTestInvocationContextProvider::createWebMvcContext),

		/**
		 * Actuator running on the WebFlux-based infrastructure.
		 */
		WEBFLUX("WebFlux", WebEndpointTestInvocationContextProvider::createWebFluxContext);

		private final String name;

		private final Function<List<Class<?>>, ConfigurableApplicationContext> contextFactory;

		Infrastructure(String name, Function<List<Class<?>>, ConfigurableApplicationContext> contextFactory) {
			this.name = name;
			this.contextFactory = contextFactory;
		}

		WebEndpointsInvocationContext createInvocationContext() {
			return new WebEndpointsInvocationContext(this.name, this.contextFactory);
		}

	}

}
