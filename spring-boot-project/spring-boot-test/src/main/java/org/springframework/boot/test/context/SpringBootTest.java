/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

/**
 * Annotation that can be specified on a test class that runs Spring Boot based tests.
 * Provides the following features over and above the regular <em>Spring TestContext
 * Framework</em>:
 * <ul>
 * <li>Uses {@link SpringBootContextLoader} as the default {@link ContextLoader} when no
 * specific {@link ContextConfiguration#loader() @ContextConfiguration(loader=...)} is
 * defined.</li>
 * <li>Automatically searches for a
 * {@link SpringBootConfiguration @SpringBootConfiguration} when nested
 * {@code @Configuration} is not used, and no explicit {@link #classes() classes} are
 * specified.</li>
 * <li>Allows custom {@link Environment} properties to be defined using the
 * {@link #properties() properties attribute}.</li>
 * <li>Provides support for different {@link #webEnvironment() webEnvironment} modes,
 * including the ability to start a fully running web server listening on a
 * {@link WebEnvironment#DEFINED_PORT defined} or {@link WebEnvironment#RANDOM_PORT
 * random} port.</li>
 * <li>Registers a {@link org.springframework.boot.test.web.client.TestRestTemplate
 * TestRestTemplate} and/or
 * {@link org.springframework.test.web.reactive.server.WebTestClient WebTestClient} bean
 * for use in web tests that are using a fully running web server.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 * @see ContextConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
public @interface SpringBootTest {

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
	 * The <em>annotated classes</em> to use for loading an
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}. Can also
	 * be specified using
	 * {@link ContextConfiguration#classes() @ContextConfiguration(classes=...)}. If no
	 * explicit classes are defined the test will look for nested
	 * {@link Configuration @Configuration} classes, before falling back to a
	 * {@link SpringBootConfiguration} search.
	 * @see ContextConfiguration#classes()
	 * @return the annotated classes used to load the application context
	 */
	Class<?>[] classes() default {};

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
		 * Creates a {@link WebApplicationContext} with a mock servlet environment if
		 * servlet APIs are on the classpath, a {@link ReactiveWebApplicationContext} if
		 * Spring WebFlux is on the classpath or a regular {@link ApplicationContext}
		 * otherwise.
		 */
		MOCK(false),

		/**
		 * Creates a web application context (reactive or servlet based) and sets a
		 * {@code server.port=0} {@link Environment} property (which usually triggers
		 * listening on a random port). Often used in conjunction with a
		 * {@link LocalServerPort} injected field on the test.
		 */
		RANDOM_PORT(true),

		/**
		 * Creates a (reactive) web application context without defining any
		 * {@code server.port=0} {@link Environment} property.
		 */
		DEFINED_PORT(true),

		/**
		 * Creates an {@link ApplicationContext} and sets
		 * {@link SpringApplication#setWebApplicationType(WebApplicationType)} to
		 * {@link WebApplicationType#NONE}.
		 */
		NONE(false);

		private final boolean embedded;

		WebEnvironment(boolean embedded) {
			this.embedded = embedded;
		}

		/**
		 * Return if the environment uses an {@link ServletWebServerApplicationContext}.
		 * @return if an {@link ServletWebServerApplicationContext} is used.
		 */
		public boolean isEmbedded() {
			return this.embedded;
		}

	}

}
