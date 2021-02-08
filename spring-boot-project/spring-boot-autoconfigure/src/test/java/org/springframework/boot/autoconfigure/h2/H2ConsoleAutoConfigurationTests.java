/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.h2;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link H2ConsoleAutoConfiguration}
 *
 * @author Andy Wilkinson
 * @author Marten Deinum
 * @author Stephane Nicoll
 * @author Shraddha Yeole
 */
class H2ConsoleAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(H2ConsoleAutoConfiguration.class));

	@Test
	void consoleIsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ServletRegistrationBean.class));
	}

	@Test
	void propertyCanEnableConsole() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled=true").run((context) -> {
			assertThat(context).hasSingleBean(ServletRegistrationBean.class);
			ServletRegistrationBean<?> registrationBean = context.getBean(ServletRegistrationBean.class);
			assertThat(registrationBean.getUrlMappings()).contains("/h2-console/*");
			assertThat(registrationBean.getInitParameters()).doesNotContainKey("trace");
			assertThat(registrationBean.getInitParameters()).doesNotContainKey("webAllowOthers");
			assertThat(registrationBean.getInitParameters()).doesNotContainKey("webAdminPassword");
		});
	}

	@Test
	void customPathMustBeginWithASlash() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled=true", "spring.h2.console.path=custom")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class)
							.hasMessageContaining("Failed to bind properties under 'spring.h2.console'");
				});
	}

	@Test
	void customPathWithTrailingSlash() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled=true", "spring.h2.console.path=/custom/")
				.run((context) -> {
					assertThat(context).hasSingleBean(ServletRegistrationBean.class);
					ServletRegistrationBean<?> registrationBean = context.getBean(ServletRegistrationBean.class);
					assertThat(registrationBean.getUrlMappings()).contains("/custom/*");
				});
	}

	@Test
	void customPath() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled=true", "spring.h2.console.path=/custom")
				.run((context) -> {
					assertThat(context).hasSingleBean(ServletRegistrationBean.class);
					ServletRegistrationBean<?> registrationBean = context.getBean(ServletRegistrationBean.class);
					assertThat(registrationBean.getUrlMappings()).contains("/custom/*");
				});
	}

	@Test
	void customInitParameters() {
		this.contextRunner.withPropertyValues("spring.h2.console.enabled=true", "spring.h2.console.settings.trace=true",
				"spring.h2.console.settings.web-allow-others=true",
				"spring.h2.console.settings.web-admin-password=abcd").run((context) -> {
					assertThat(context).hasSingleBean(ServletRegistrationBean.class);
					ServletRegistrationBean<?> registrationBean = context.getBean(ServletRegistrationBean.class);
					assertThat(registrationBean.getUrlMappings()).contains("/h2-console/*");
					assertThat(registrationBean.getInitParameters()).containsEntry("trace", "");
					assertThat(registrationBean.getInitParameters()).containsEntry("webAllowOthers", "");
					assertThat(registrationBean.getInitParameters()).containsEntry("webAdminPassword", "abcd");
				});
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void dataSourceUrlIsLoggedWhenAvailable(CapturedOutput output) throws BeansException {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.h2.console.enabled=true").run((context) -> {
					try (Connection connection = context.getBean(DataSource.class).getConnection()) {
						assertThat(output)
								.contains("Database available at '" + connection.getMetaData().getURL() + "'");
					}
				});
	}

	@Test
	void h2ConsoleShouldNotFailIfDatabaseConnectionFails() {
		this.contextRunner.withUserConfiguration(CustomDataSourceConfiguration.class)
				.withPropertyValues("spring.h2.console.enabled=true")
				.run((context) -> assertThat(context.isRunning()).isTrue());
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDataSourceConfiguration {

		@Bean
		DataSource dataSource() throws SQLException {
			DataSource dataSource = mock(DataSource.class);
			given(dataSource.getConnection()).willThrow(IllegalStateException.class);
			return dataSource;
		}

	}

}
