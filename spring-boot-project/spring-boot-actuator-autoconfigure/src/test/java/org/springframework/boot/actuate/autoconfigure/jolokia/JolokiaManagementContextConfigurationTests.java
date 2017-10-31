/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.jolokia;

import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link JolokiaManagementContextConfiguration}.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class JolokiaManagementContextConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(ManagementContextAutoConfiguration.class,
							ServletManagementContextAutoConfiguration.class,
							JolokiaManagementContextConfiguration.class));

	@Test
	public void jolokiaCanBeEnabled() {
		this.contextRunner.withPropertyValues("management.jolokia.enabled=true")
				.run((context) -> {
					context.getBean(ServletRegistrationBean.class);
					assertThat(context).hasSingleBean(ServletRegistrationBean.class);
					ServletRegistrationBean<?> registrationBean = context
							.getBean(ServletRegistrationBean.class);
					assertThat(registrationBean.getUrlMappings())
							.contains("/application/jolokia/*");
					assertThat(registrationBean.getInitParameters()).isEmpty();
				});
	}

	@Test
	public void jolokiaIsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(ServletRegistrationBean.class));
	}

	@Test
	public void customPath() {
		this.contextRunner
				.withPropertyValues("management.jolokia.enabled=true",
						"management.jolokia.path=/lokia")
				.run(isDefinedOnPath("/application/lokia/*"));
	}

	@Test
	public void customManagementPath() {
		this.contextRunner
				.withPropertyValues("management.jolokia.enabled=true",
						"management.endpoints.web.base-path=/admin")
				.run(isDefinedOnPath("/admin/jolokia/*"));
	}

	@Test
	public void customInitParameters() {
		this.contextRunner.withPropertyValues("management.jolokia.enabled=true",
				"management.jolokia.config.debug=true").run((context) -> {
					assertThat(context).hasSingleBean(ServletRegistrationBean.class);
					ServletRegistrationBean<?> registrationBean = context
							.getBean(ServletRegistrationBean.class);
					assertThat(registrationBean.getInitParameters())
							.containsOnly(entry("debug", "true"));
				});
	}

	private ContextConsumer<AssertableWebApplicationContext> isDefinedOnPath(
			String path) {
		return (context) -> {
			assertThat(context).hasSingleBean(ServletRegistrationBean.class);
			ServletRegistrationBean<?> registrationBean = context
					.getBean(ServletRegistrationBean.class);
			assertThat(registrationBean.getUrlMappings()).containsExactly(path);
		};
	}

}
