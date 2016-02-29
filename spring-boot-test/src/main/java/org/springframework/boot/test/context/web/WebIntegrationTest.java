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

package org.springframework.boot.test.context.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.boot.test.context.IntegrationTest;
import org.springframework.boot.test.context.SpringApplicationConfiguration;
import org.springframework.boot.test.context.SpringApplicationContextLoader;
import org.springframework.boot.test.context.SpringApplicationTest;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class annotation signifying that the tests are "web integration tests" for a
 * {@link org.springframework.boot.SpringApplication Spring Boot Application} and
 * therefore require full startup in the same way as a production application (listening
 * on normal ports. By default will load nested {@code @Configuration} classes, or
 * fallback an {@link SpringApplicationConfiguration @SpringApplicationConfiguration}
 * search. Unless otherwise configured, a {@link SpringApplicationContextLoader} will be
 * used to load the {@link ApplicationContext}. Use
 * {@link SpringApplicationConfiguration @SpringApplicationConfiguration} or
 * {@link ContextConfiguration @ContextConfiguration} if custom configuration is required.
 * <p>
 * If you are not testing a web application consider using the
 * {@link IntegrationTest @IntegrationTest} annotation instead. If you are testing a web
 * application and want to mock the servlet environment (for example so that you can use
 * {@link MockMvc}) you should switch to the
 * {@link SpringApplicationTest @SpringApplicationTest} annotation.
 * <p>
 * Tests that need to make REST calls to the started server can additionally
 * {@link Autowire @Autowire} a {@link TestRestTemplate} which will be pre-configured with
 * a {@link LocalHostUriTemplateHandler}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see SpringApplicationTest
 * @see IntegrationTest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(WebIntegrationTestContextBootstrapper.class)
public @interface WebIntegrationTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return properties to add to the context
	 */
	String[] value() default {};

	/**
	 * Convenience attribute that can be used to set a {@code server.port=0}
	 * {@link Environment} property which usually triggers listening on a random port.
	 * Often used in conjunction with a <code>&#064;Value("${local.server.port}")</code>
	 * injected field on the test.
	 * @return if a random port should be used
	 */
	boolean randomPort() default false;

}
