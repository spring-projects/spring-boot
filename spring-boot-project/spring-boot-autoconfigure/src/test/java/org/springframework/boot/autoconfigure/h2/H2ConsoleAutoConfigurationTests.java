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

package org.springframework.boot.autoconfigure.h2;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link H2ConsoleAutoConfiguration}
 *
 * @author Andy Wilkinson
 * @author Marten Deinum
 * @author Stephane Nicoll
 */
public class H2ConsoleAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(H2ConsoleAutoConfiguration.class));

	@Test
	public void consoleIsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(
				context.getBeansOfType(ServletRegistrationBean.class)).isEmpty());
	}

	@Test
	public void propertyCanEnableConsole() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled=true").run((context) -> {
			assertThat(context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
			ServletRegistrationBean<?> registrationBean = context
					.getBean(ServletRegistrationBean.class);
			assertThat(registrationBean.getUrlMappings()).contains("/h2-console/*");
			assertThat(registrationBean.getInitParameters()).doesNotContainKey("trace");
			assertThat(registrationBean.getInitParameters())
					.doesNotContainKey("webAllowOthers");
		});
	}

	@Test
	public void customPathMustBeginWithASlash() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled:true",
				"spring.h2.console.path:custom")
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class).hasMessageContaining(
								"Failed to bind properties under 'spring.h2.console'"));
	}

	@Test
	public void customPathWithTrailingSlash() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled:true",
				"spring.h2.console.path:/custom/")
				.run((context) -> {
					assertThat(context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
					ServletRegistrationBean<?> servletRegistrationBean = context
							.getBean(ServletRegistrationBean.class);
					assertThat(servletRegistrationBean.getUrlMappings()).contains("/custom/*");
				});
	}

	@Test
	public void customPathMustNotHaveOnlySlash() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled:true",
				"spring.h2.console.path:/")
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class).hasMessageContaining(
								"Failed to bind properties under 'spring.h2.console'"));
	}

	@Test
	public void customPath() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled:true",
				"spring.h2.console.path:/custom")
				.run((context) -> {
					assertThat(context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
					ServletRegistrationBean<?> servletRegistrationBean = context
							.getBean(ServletRegistrationBean.class);
					assertThat(servletRegistrationBean.getUrlMappings()).contains("/custom/*");
				});
	}

	@Test
	public void customInitParameters() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled:true",
				"spring.h2.console.settings.trace=true",
				"spring.h2.console.settings.webAllowOthers=true")
				.run((context) -> {
					assertThat(context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
					ServletRegistrationBean<?> registrationBean = context
							.getBean(ServletRegistrationBean.class);
					assertThat(registrationBean.getUrlMappings()).contains("/h2-console/*");
					assertThat(registrationBean.getInitParameters()).containsEntry("trace", "");
					assertThat(registrationBean.getInitParameters()).containsEntry("webAllowOthers",
							"");
				});
	}

}
