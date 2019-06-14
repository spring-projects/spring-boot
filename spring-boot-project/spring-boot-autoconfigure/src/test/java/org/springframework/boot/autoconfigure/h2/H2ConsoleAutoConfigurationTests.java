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

package org.springframework.boot.autoconfigure.h2;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link H2ConsoleAutoConfiguration}
 *
 * @author Andy Wilkinson
 * @author Marten Deinum
 * @author Stephane Nicoll
 */
class H2ConsoleAutoConfigurationTests {

	private AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();

	@BeforeEach
	public void setupContext() {
		this.context.setServletContext(new MockServletContext());
	}

	@AfterEach
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void consoleIsDisabledByDefault() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class)).isEmpty();
	}

	@Test
	void propertyCanEnableConsole() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		TestPropertyValues.of("spring.h2.console.enabled:true").applyTo(this.context);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
		ServletRegistrationBean<?> registrationBean = this.context.getBean(ServletRegistrationBean.class);
		assertThat(registrationBean.getUrlMappings()).contains("/h2-console/*");
		assertThat(registrationBean.getInitParameters()).doesNotContainKey("trace");
		assertThat(registrationBean.getInitParameters()).doesNotContainKey("webAllowOthers");
	}

	@Test
	void customPathMustBeginWithASlash() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		TestPropertyValues.of("spring.h2.console.enabled:true", "spring.h2.console.path:custom").applyTo(this.context);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(this.context::refresh)
				.withMessageContaining("Failed to bind properties under 'spring.h2.console'");
	}

	@Test
	void customPathWithTrailingSlash() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		TestPropertyValues.of("spring.h2.console.enabled:true", "spring.h2.console.path:/custom/")
				.applyTo(this.context);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
		ServletRegistrationBean<?> servletRegistrationBean = this.context.getBean(ServletRegistrationBean.class);
		assertThat(servletRegistrationBean.getUrlMappings()).contains("/custom/*");
	}

	@Test
	void customPath() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		TestPropertyValues.of("spring.h2.console.enabled:true", "spring.h2.console.path:/custom").applyTo(this.context);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
		ServletRegistrationBean<?> servletRegistrationBean = this.context.getBean(ServletRegistrationBean.class);
		assertThat(servletRegistrationBean.getUrlMappings()).contains("/custom/*");
	}

	@Test
	void customInitParameters() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		TestPropertyValues.of("spring.h2.console.enabled:true", "spring.h2.console.settings.trace=true",
				"spring.h2.console.settings.webAllowOthers=true").applyTo(this.context);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
		ServletRegistrationBean<?> registrationBean = this.context.getBean(ServletRegistrationBean.class);
		assertThat(registrationBean.getUrlMappings()).contains("/h2-console/*");
		assertThat(registrationBean.getInitParameters()).containsEntry("trace", "");
		assertThat(registrationBean.getInitParameters()).containsEntry("webAllowOthers", "");
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void dataSourceUrlIsLoggedWhenAvailable(CapturedOutput capturedOutput) throws BeansException, SQLException {
		this.context.register(DataSourceAutoConfiguration.class, H2ConsoleAutoConfiguration.class);
		TestPropertyValues.of("spring.h2.console.enabled:true").applyTo(this.context);
		this.context.refresh();
		try (Connection connection = this.context.getBean(DataSource.class).getConnection()) {
			assertThat(capturedOutput).contains("Database available at '" + connection.getMetaData().getURL() + "'");
		}
	}

}
