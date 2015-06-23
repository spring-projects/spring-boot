/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DevToolHomePropertiesPostProcessor}.
 *
 * @author Phillip Webb
 */
public class DevToolHomePropertiesPostProcessorTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private File home;

	@Before
	public void setup() throws IOException {
		this.home = this.temp.newFolder();
	}

	@Test
	public void loadsHomeProperties() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		OutputStream out = new FileOutputStream(new File(this.home,
				".spring-boot-devtools.properties"));
		properties.store(out, null);
		out.close();
		Environment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		postProcessor.setEnvironment(environment);
		postProcessor.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class));
		assertThat(environment.getProperty("abc"), equalTo("def"));
	}

	@Test
	public void ignoresMissingHomeProperties() throws Exception {
		Environment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		postProcessor.setEnvironment(environment);
		postProcessor.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class));
		assertThat(environment.getProperty("abc"), nullValue());
	}

	private class MockDevToolHomePropertiesPostProcessor extends
			DevToolHomePropertiesPostProcessor {

		@Override
		protected File getHomeFolder() {
			return DevToolHomePropertiesPostProcessorTests.this.home;
		}
	}

}
