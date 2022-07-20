/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.graphql;

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
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Annotation to perform GraphQL tests focusing on GraphQL request execution without a Web
 * layer, and loading only a subset of the application configuration.
 * <p>
 * The annotation disables full auto-configuration and instead loads only components
 * relevant to GraphQL tests, including the following:
 * <ul>
 * <li>{@code @Controller}
 * <li>{@code RuntimeWiringConfigurer}
 * <li>{@code @JsonComponent}
 * <li>{@code Converter}
 * <li>{@code GenericConverter}
 * <li>{@code DataFetcherExceptionResolver}
 * <li>{@code Instrumentation}
 * <li>{@code GraphQlSourceBuilderCustomizer}
 * </ul>
 * <p>
 * The annotation does not automatically load {@code @Component}, {@code @Service},
 * {@code @Repository}, and other beans.
 * <p>
 * By default, tests annotated with {@code @GraphQlTest} have a
 * {@link org.springframework.graphql.test.tester.GraphQlTester} configured. For more
 * fine-grained control of the GraphQlTester, use
 * {@link AutoConfigureGraphQlTester @AutoConfigureGraphQlTester}.
 * <p>
 * Typically {@code @GraphQlTest} is used in combination with
 * {@link org.springframework.boot.test.mock.mockito.MockBean @MockBean} or
 * {@link org.springframework.context.annotation.Import @Import} to load any collaborators
 * and other components required for the tests.
 * <p>
 * To load your full application configuration instead and test via
 * {@code HttpGraphQlTester}, consider using
 * {@link org.springframework.boot.test.context.SpringBootTest @SpringBootTest} combined
 * with {@link AutoConfigureHttpGraphQlTester @AutoConfigureHttpGraphQlTester}.
 *
 * @author Brian Clozel
 * @since 2.7.0
 * @see AutoConfigureGraphQlTester
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(GraphQlTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(GraphQlTypeExcludeFilter.class)
@AutoConfigureCache
@AutoConfigureJson
@AutoConfigureGraphQl
@AutoConfigureGraphQlTester
@ImportAutoConfiguration
public @interface GraphQlTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the properties to add
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
	 * {@link SpringBootApplication @SpringBootApplication}. By default, only
	 * {@code @Controller} (when no explicit {@link #controllers() controllers} are
	 * defined), {@code RuntimeWiringConfigurer}, {@code @JsonComponent},
	 * {@code Converter}, {@code GenericConverter}, {@code DataFetcherExceptionResolver},
	 * {@code Instrumentation} and {@code GraphQlSourceBuilderCustomizer} beans are
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
