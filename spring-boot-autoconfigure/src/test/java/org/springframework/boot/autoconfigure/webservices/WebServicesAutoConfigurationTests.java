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

package org.springframework.boot.autoconfigure.webservices;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.WebApplicationContextTester;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServicesAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
public class WebServicesAutoConfigurationTests {

	private final WebApplicationContextTester context = new WebApplicationContextTester()
			.withConfiguration(AutoConfigurations.of(WebServicesAutoConfiguration.class));

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void defaultConfiguration() {
		this.context.run((loaded) -> assertThat(loaded)
				.hasSingleBean(ServletRegistrationBean.class));
	}

	@Test
	public void customPathMustBeginWithASlash() {
		this.context.withPropertyValues("spring.webservices.path=invalid")
				.run((loaded) -> {
					assertThat(loaded).getFailure()
							.isInstanceOf(BeanCreationException.class)
							.hasMessageContaining(
									"Failed to bind properties under 'spring.webservices'");
				});
	}

	@Test
	public void customPath() {
		this.context.withPropertyValues("spring.webservices.path=/valid")
				.run((loaded) -> assertThat(getUrlMappings(loaded)).contains("/valid/*"));
	}

	@Test
	public void customPathWithTrailingSlash() {
		this.context.withPropertyValues("spring.webservices.path=/valid/")
				.run((loaded) -> assertThat(getUrlMappings(loaded)).contains("/valid/*"));
	}

	@Test
	public void customLoadOnStartup() {
		this.context.withPropertyValues("spring.webservices.servlet.load-on-startup=1")
				.run((loaded) -> {
					ServletRegistrationBean<?> registrationBean = loaded
							.getBean(ServletRegistrationBean.class);
					assertThat(ReflectionTestUtils.getField(registrationBean,
							"loadOnStartup")).isEqualTo(1);
				});
	}

	@Test
	public void customInitParameters() {
		this.context
				.withPropertyValues("spring.webservices.servlet.init.key1=value1",
						"spring.webservices.servlet.init.key2=value2")
				.run(loaded -> assertThat(
						getServletRegistrationBean(loaded).getInitParameters())
								.containsEntry("key1", "value1")
								.containsEntry("key2", "value2"));
	}

	private Collection<String> getUrlMappings(ApplicationContext loaded) {
		return getServletRegistrationBean(loaded).getUrlMappings();
	}

	private ServletRegistrationBean<?> getServletRegistrationBean(
			ApplicationContext loaded) {
		return loaded.getBean(ServletRegistrationBean.class);
	}

}
