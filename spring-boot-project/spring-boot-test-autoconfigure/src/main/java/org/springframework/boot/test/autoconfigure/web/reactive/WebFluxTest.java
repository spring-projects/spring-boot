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

package org.springframework.boot.test.autoconfigure.web.reactive;

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
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Annotation that can be used for a Spring WebFlux test that focuses
 * <strong>only</strong> on Spring WebFlux components.
 * <p>
 * Using this annotation only enables auto-configuration that is relevant to WebFlux
 * tests. Similarly, component scanning is limited to beans annotated with:
 * <ul>
 * <li>{@code @Controller}</li>
 * <li>{@code @ControllerAdvice}</li>
 * <li>{@code @JsonComponent}</li>
 * </ul>
 * <p>
 * as well as beans that implement:
 * <ul>
 * <li>{@code Converter}</li>
 * <li>{@code GenericConverter}</li>
 * <li>{@code IDialect}, if Thymeleaf is available</li>
 * <li>{@code Module}, if Jackson is available</li>
 * <li>{@code WebExceptionHandler}</li>
 * <li>{@code WebFluxConfigurer}</li>
 * <li>{@code WebFilter}</li>
 * </ul>
 * <p>
 * By default, tests annotated with {@code @WebFluxTest} will also auto-configure a
 * {@link WebTestClient}. For more fine-grained control of WebTestClient the
 * {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient} annotation can be used.
 * <p>
 * Typically {@code @WebFluxTest} is used in combination with
 * {@link org.springframework.test.context.bean.override.mockito.MockitoBean @MockitoBean}
 * or {@link Import @Import} to create any collaborators required by your
 * {@code @Controller} beans.
 * <p>
 * If you are looking to load your full application configuration and use WebTestClient,
 * you should consider {@link SpringBootTest @SpringBootTest} combined with
 * {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient} rather than this
 * annotation.
 * <p>
 * When using JUnit 4, this annotation should be used in combination with
 * {@code @RunWith(SpringRunner.class)}.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @since 2.0.0
 * @see AutoConfigureWebFlux
 * @see AutoConfigureWebTestClient
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(WebFluxTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(WebFluxTypeExcludeFilter.class)
@AutoConfigureCache
@AutoConfigureJson
@AutoConfigureWebFlux
@AutoConfigureWebTestClient
@ImportAutoConfiguration
public @interface WebFluxTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the properties to add
	 * @since 2.1.0
	 */
	String[] properties() default {};

	/**
	 * Specifies the controllers to test. This is an alias of {@link #controllers()} which
	 * can be used for brevity if no other attributes are defined. See
	 * {@link #controllers()} for details.
	 * @see #controllers()
	 * @return the controllers to test
	 */
	@AliasFor("controllers")
	Class<?>[] value() default {};

	/**
	 * Specifies the controllers to test. May be left blank if all {@code @Controller}
	 * beans should be added to the application context.
	 * @see #value()
	 * @return the controllers to test
	 */
	@AliasFor("value")
	Class<?>[] controllers() default {};

	/**
	 * Determines if default filtering should be used with
	 * {@link SpringBootApplication @SpringBootApplication}. By default only
	 * {@code @Controller} (when no explicit {@link #controllers() controllers} are
	 * defined), {@code @ControllerAdvice} and {@code WebFluxConfigurer} beans are
	 * included.
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
