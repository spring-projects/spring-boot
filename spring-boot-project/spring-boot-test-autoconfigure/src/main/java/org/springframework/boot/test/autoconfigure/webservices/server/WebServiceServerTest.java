/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.webservices.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Annotation that can be used for a typical Spring web service server test. Can be used
 * when a test focuses <strong>only</strong> on Spring WS endpoints.
 * <p>
 * Using this annotation only enables auto-configuration that is relevant to Web Service
 * Server tests. Similarly, component scanning is limited to beans annotated with:
 * <ul>
 * <li>{@code @Endpoint}</li>
 * </ul>
 * <p>
 * as well as beans that implement:
 * <ul>
 * <li>{@code EndpointInterceptor}</li>
 * </ul>
 * <p>
 * Typically {@code WebServiceServerTest} is used in combination with
 * {@link org.springframework.test.context.bean.override.mockito.MockitoBean @MockitoBean}
 * or {@link org.springframework.context.annotation.Import @Import} to create any
 * collaborators required by your {@code Endpoint} beans.
 * <p>
 * If you are looking to load your full application configuration and use
 * MockWebServiceClient, you should consider
 * {@link org.springframework.boot.test.context.SpringBootTest @SpringBootTest} combined
 * with {@link AutoConfigureMockWebServiceClient @AutoConfigureMockWebServiceClient}
 * rather than this annotation.
 *
 * @author Daniil Razorenov
 * @since 2.6.0
 * @see AutoConfigureMockWebServiceClient
 * @see AutoConfigureWebServiceServer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(WebServiceServerTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(WebServiceServerTypeExcludeFilter.class)
@AutoConfigureWebServiceServer
@AutoConfigureMockWebServiceClient
@ImportAutoConfiguration
public @interface WebServiceServerTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the properties to add
	 */
	String[] properties() default {};

	/**
	 * Specifies the endpoints to test. This is an alias of {@link #endpoints()} which can
	 * be used for brevity if no other attributes are defined. See {@link #endpoints()}
	 * for details.
	 * @return the endpoints to test
	 * @see #endpoints()
	 */
	@AliasFor("endpoints")
	Class<?>[] value() default {};

	/**
	 * Specifies the endpoints to test. May be left blank if all {@code @Endpoint} beans
	 * should be added to the application context.
	 * @return the endpoints to test
	 * @see #value()
	 */
	@AliasFor("value")
	Class<?>[] endpoints() default {};

	/**
	 * Determines if default filtering should be used with
	 * {@link SpringBootApplication @SpringBootApplication}. By default only
	 * {@code @Endpoint} (when no explicit {@link #endpoints() controllers} are defined)
	 * are included.
	 * @see #includeFilters()
	 * @see #excludeFilters()
	 * @return if default filters should be used
	 */
	boolean useDefaultFilters() default true;

	/**
	 * A set of include filters which can be used to add otherwise filtered beans to the
	 * application context.
	 * @return include filters to apply
	 */
	ComponentScan.Filter[] includeFilters() default {};

	/**
	 * A set of exclude filters which can be used to filter beans that would otherwise be
	 * added to the application context.
	 * @return exclude filters to apply
	 */
	ComponentScan.Filter[] excludeFilters() default {};

	/**
	 * Auto-configuration exclusions that should be applied for this test.
	 * @return auto-configuration exclusions to apply
	 */
	@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
	Class<?>[] excludeAutoConfiguration() default {};

}
