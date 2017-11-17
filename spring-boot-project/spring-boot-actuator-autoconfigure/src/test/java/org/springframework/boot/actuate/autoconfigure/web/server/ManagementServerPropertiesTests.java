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

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementServerProperties}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ManagementServerPropertiesTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultManagementServerProperties() {
		ManagementServerProperties properties = new ManagementServerProperties();
		assertThat(properties.getPort()).isNull();
		assertThat(properties.getContextPath()).isEqualTo("");
		assertThat(properties.getAddApplicationContextHeader()).isEqualTo(false);
	}

	@Test
	public void definedManagementServerProperties() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setPort(123);
		properties.setContextPath("/foo");
		assertThat(properties.getPort()).isEqualTo(123);
		assertThat(properties.getContextPath()).isEqualTo("/foo");
	}

	@Test
	public void trailingSlashOfContextPathIsRemoved() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setContextPath("/foo/");
		assertThat(properties.getContextPath()).isEqualTo("/foo");
	}

	@Test
	public void slashOfContextPathIsDefaultValue() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setContextPath("/");
		assertThat(properties.getContextPath()).isEqualTo("");
	}

	public ManagementServerProperties load(String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(Config.class);
		ctx.refresh();
		this.context = ctx;
		return this.context.getBean(ManagementServerProperties.class);
	}

	@EnableConfigurationProperties(ManagementServerProperties.class)
	protected static class Config {

	}

}
