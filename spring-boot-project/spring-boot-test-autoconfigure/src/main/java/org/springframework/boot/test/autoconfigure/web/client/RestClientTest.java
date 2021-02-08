/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.client;

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
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Annotation for a Spring rest client test that focuses <strong>only</strong> on beans
 * that use {@link RestTemplateBuilder}.
 * <p>
 * Using this annotation will disable full auto-configuration and instead apply only
 * configuration relevant to rest client tests (i.e. Jackson or GSON auto-configuration
 * and {@code @JsonComponent} beans, but not regular {@link Component @Component} beans).
 * <p>
 * By default, tests annotated with {@code RestClientTest} will also auto-configure a
 * {@link MockRestServiceServer}. For more fine-grained control the
 * {@link AutoConfigureMockRestServiceServer @AutoConfigureMockRestServiceServer}
 * annotation can be used.
 * <p>
 * If you are testing a bean that doesn't use {@link RestTemplateBuilder} but instead
 * injects a {@link RestTemplate} directly, you can add
 * {@code @AutoConfigureWebClient(registerRestTemplate=true)}.
 * <p>
 * When using JUnit 4, this annotation should be used in combination with
 * {@code @RunWith(SpringRunner.class)}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @since 1.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(RestClientTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(RestClientTypeExcludeFilter.class)
@AutoConfigureCache
@AutoConfigureWebClient
@AutoConfigureMockRestServiceServer
@ImportAutoConfiguration
public @interface RestClientTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the properties to add
	 * @since 2.1.0
	 */
	String[] properties() default {};

	/**
	 * Specifies the components to test. This is an alias of {@link #components()} which
	 * can be used for brevity if no other attributes are defined. See
	 * {@link #components()} for details.
	 * @see #components()
	 * @return the components to test
	 */
	@AliasFor("components")
	Class<?>[] value() default {};

	/**
	 * Specifies the components to test. May be left blank if components will be manually
	 * imported or created directly.
	 * @see #value()
	 * @return the components to test
	 */
	@AliasFor("value")
	Class<?>[] components() default {};

	/**
	 * Determines if default filtering should be used with
	 * {@link SpringBootApplication @SpringBootApplication}. By default only
	 * {@code @JsonComponent} and {@code Module} beans are included.
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
