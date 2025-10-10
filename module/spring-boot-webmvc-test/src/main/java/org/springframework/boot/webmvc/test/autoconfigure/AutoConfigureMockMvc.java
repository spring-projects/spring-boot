/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.htmlunit.WebClient;
import org.openqa.selenium.WebDriver;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.PropertyMapping;
import org.springframework.boot.test.context.PropertyMapping.Skip;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of {@link MockMvc}. If AssertJ is available a {@link MockMvcTester}
 * is auto-configured as well.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see MockMvcAutoConfiguration
 * @see SpringBootMockMvcBuilderCustomizer
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("spring.test.mockmvc")
public @interface AutoConfigureMockMvc {

	/**
	 * If filters from the application context should be registered with MockMVC. Defaults
	 * to {@code true}.
	 * @return if filters should be added
	 */
	boolean addFilters() default true;

	/**
	 * How {@link MvcResult} information should be printed after each MockMVC invocation.
	 * @return how information is printed
	 */
	@PropertyMapping(skip = Skip.ON_DEFAULT_VALUE)
	MockMvcPrint print() default MockMvcPrint.DEFAULT;

	/**
	 * If {@link MvcResult} information should be printed only if the test fails.
	 * @return {@code true} if printing only occurs on failure
	 */
	boolean printOnlyOnFailure() default true;

	/**
	 * If a {@link WebClient} should be auto-configured when HtmlUnit is on the classpath.
	 * Defaults to {@code true}.
	 * @return if a {@link WebClient} is auto-configured
	 */
	@PropertyMapping("webclient.enabled")
	boolean webClientEnabled() default true;

	/**
	 * If a {@link WebDriver} should be auto-configured when Selenium is on the classpath.
	 * Defaults to {@code true}.
	 * @return if a {@link WebDriver} is auto-configured
	 */
	@PropertyMapping("webdriver.enabled")
	boolean webDriverEnabled() default true;

}
