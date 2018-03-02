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

package org.springframework.boot.test.autoconfigure.web.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Annotation that can be used in combination with {@code @RunWith(SpringRunner.class)}
 * for a typical Spring MVC test. Can be used when a test focuses <strong>only</strong> on
 * Spring MVC components.
 * <p>
 * Using this annotation will disable full auto-configuration and instead apply only
 * configuration relevant to MVC tests (i.e. {@code @Controller},
 * {@code @ControllerAdvice}, {@code @JsonComponent},
 * {@code Converter}/{@code GenericConverter}, {@code Filter}, {@code WebMvcConfigurer}
 * and {@code HandlerMethodArgumentResolver} beans but not {@code @Component},
 * {@code @Service} or {@code @Repository} beans).
 * <p>
 * By default, tests annotated with {@code @WebMvcTest} will also auto-configure Spring
 * Security and {@link MockMvc} (include support for HtmlUnit WebClient and Selenium
 * WebDriver). For more fine-grained control of MockMVC the
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} annotation can be used.
 * <p>
 * Typically {@code @WebMvcTest} is used in combination with {@link MockBean @MockBean} or
 * {@link Import @Import} to create any collaborators required by your {@code @Controller}
 * beans.
 * <p>
 * If you are looking to load your full application configuration and use MockMVC, you
 * should consider {@link SpringBootTest @SpringBootTest} combined with
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} rather than this annotation.
 *
 * @author Phillip Webb
 * @see AutoConfigureWebMvc
 * @see AutoConfigureMockMvc
 * @see AutoConfigureCache
 * @since 1.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(WebMvcTestContextBootstrapper.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(WebMvcTypeExcludeFilter.class)
@AutoConfigureCache
@AutoConfigureWebMvc
@AutoConfigureMockMvc
@ImportAutoConfiguration
public @interface WebMvcTest {

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
	 * defined), {@code @ControllerAdvice} and {@code WebMvcConfigurer} beans are
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
	Filter[] includeFilters() default {};

	/**
	 * A set of exclude filters which can be used to filter beans that would otherwise be
	 * added to the application context.
	 * @return exclude filters to apply
	 */
	Filter[] excludeFilters() default {};

	/**
	 * If Spring Security's {@link MockMvc} support should be auto-configured when it is
	 * on the classpath. Defaults to {@code true}.
	 * @return if Spring Security's MockMvc support is auto-configured
	 */
	@AliasFor(annotation = AutoConfigureMockMvc.class)
	boolean secure() default true;

	/**
	 * Auto-configuration exclusions that should be applied for this test.
	 * @return auto-configuration exclusions to apply
	 */
	@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
	Class<?>[] excludeAutoConfiguration() default {};

}
