/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ManagementServerPropertiesAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ManagementServerPropertiesAutoConfigurationTests {

	@Test
	public void defaultManagementServerProperties() {
		ManagementServerProperties properties = new ManagementServerProperties();
		assertThat(properties.getPort(), nullValue());
		assertThat(properties.getContextPath(), equalTo(""));
	}

	@Test
	public void definedManagementServerProperties() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setPort(123);
		properties.setContextPath("/foo");
		assertThat(properties.getPort(), equalTo(123));
		assertThat(properties.getContextPath(), equalTo("/foo"));
	}

	@Test
	public void trailingSlashOfContextPathIsRemoved() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setContextPath("/foo/");
		assertThat(properties.getContextPath(), equalTo("/foo"));
	}

	@Test
	public void slashOfContextPathIsDefaultValue() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setContextPath("/");
		assertThat(properties.getContextPath(), equalTo(""));
	}

}
