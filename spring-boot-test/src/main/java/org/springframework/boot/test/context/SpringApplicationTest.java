/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.web.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test class annotation signifying that the tests are for a
 * {@link org.springframework.boot.SpringApplication Spring Boot Application}. By default
 * will load nested {@code @Configuration} classes, or fall back on
 * {@link SpringApplicationConfiguration @SpringApplicationConfiguration} search. Unless
 * otherwise configured, a {@link SpringApplicationContextLoader} will be used to load the
 * {@link ApplicationContext}. Use
 * {@link SpringApplicationConfiguration @SpringApplicationConfiguration} or
 * {@link ContextConfiguration @ContextConfiguration} if custom configuration is required.
 * <p>
 * The environment that will be used can be configured using the {@code mode} attribute.
 * By default, a mock servlet environment will be used when this annotation is used to a
 * test web application. If you want to start a real embedded servlet container in the
 * same way as a production application (listening on normal ports) configure
 * {@code mode=EMBEDDED_SERVLET}. If want to disable the creation of the mock servlet
 * environment, configure {@code mode=STANDARD}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(SpringApplicationTestContextBootstrapper.class)
public @interface SpringApplicationTest {

	/**
	 * Alias for {@link #properties()}.
	 * @return the properties to apply
	 */
	@AliasFor("properties")
	String[] value() default {};

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the properties to add
	 */
	@AliasFor("value")
	String[] properties() default {};

	/**
	 * The type of web environment to create when applicable. Defaults to
	 * {@link WebEnvironment#MOCK}.
	 * @return the type of web environment
	 */
	WebEnvironment webEnvironment() default WebEnvironment.MOCK;

	/**
	 * An enumeration web environment modes.
	 */
	enum WebEnvironment {

		/**
		 * Creates a {@link WebApplicationContext} with a mock servlet environment or a
		 * regular {@link ApplicationContext} if servlet APIs are not on the classpath.
		 */
		MOCK(false),

		/**
		 * Creates an {@link EmbeddedWebApplicationContext} and sets a
		 * {@code server.port=0} {@link Environment} property (which usually triggers
		 * listening on a random port). Often used in conjunction with a
		 * {@link LocalServerPort} injected field on the test.
		 */
		RANDOM_PORT(true),

		/**
		 * Creates an {@link EmbeddedWebApplicationContext} without defining any
		 * {@code server.port=0} {@link Environment} property.
		 */
		DEFINED_PORT(true),

		/**
		 * Creates an {@link ApplicationContext} and sets
		 * {@link SpringApplication#setWebEnvironment(boolean)} to {@code false}.
		 */
		NONE(false);

		private final boolean embedded;

		WebEnvironment(boolean embedded) {
			this.embedded = embedded;
		}

		/**
		 * Return if the environment uses an {@link EmbeddedWebApplicationContext}.
		 * @return if an {@link EmbeddedWebApplicationContext} is used.
		 */
		public boolean isEmbedded() {
			return this.embedded;
		}

	}

}
