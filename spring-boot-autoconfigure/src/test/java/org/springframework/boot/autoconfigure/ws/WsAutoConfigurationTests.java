/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.ws;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WsAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class WsAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		load(WsAutoConfiguration.class);

		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
	}

	@Test
	public void customPathMustBeginWithASlash() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("Path must start with /");
		load(WsAutoConfiguration.class, "spring.ws.path=invalid");
	}

	@Test
	public void customPathWithTrailingSlash() {
		load(WsAutoConfiguration.class, "spring.ws.path=/valid/");
		assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings())
				.contains("/valid/*");
	}

	@Test
	public void customPath() {
		load(WsAutoConfiguration.class, "spring.ws.path=/valid");
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
		assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings())
				.contains("/valid/*");
	}

	@Test
	public void customLoadOnStartup() {
		load(WsAutoConfiguration.class, "spring.ws.servlet.load-on-startup=1");
		ServletRegistrationBean registrationBean = this.context
				.getBean(ServletRegistrationBean.class);
		assertThat(ReflectionTestUtils.getField(registrationBean, "loadOnStartup"))
				.isEqualTo(1);
	}

	@Test
	public void customInitParameters() {
		load(WsAutoConfiguration.class, "spring.ws.servlet.init.key1=value1",
				"spring.ws.servlet.init.key2=value2");
		ServletRegistrationBean registrationBean = this.context
				.getBean(ServletRegistrationBean.class);
		assertThat(registrationBean.getInitParameters()).containsEntry("key1", "value1");
		assertThat(registrationBean.getInitParameters()).containsEntry("key2", "value2");
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.setServletContext(new MockServletContext());
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.register(config);
		ctx.refresh();
		this.context = ctx;
	}

}
