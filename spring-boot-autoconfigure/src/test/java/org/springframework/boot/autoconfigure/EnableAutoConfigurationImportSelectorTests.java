/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnableAutoConfigurationImportSelector}
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
@SuppressWarnings("deprecation")
public class EnableAutoConfigurationImportSelectorTests {

	private final EnableAutoConfigurationImportSelector importSelector = new EnableAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final MockEnvironment environment = new MockEnvironment();

	@Before
	public void setup() {
		this.importSelector.setBeanFactory(this.beanFactory);
		this.importSelector.setEnvironment(this.environment);
		this.importSelector.setResourceLoader(new DefaultResourceLoader());
	}

	@Test
	public void propertyOverrideSetToTrue() throws Exception {
		this.environment.setProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY,
				"true");
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).isNotEmpty();
	}

	@Test
	public void propertyOverrideSetToFalse() throws Exception {
		this.environment.setProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY,
				"false");
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).isEmpty();
	}

	private String[] selectImports(Class<?> source) {
		return this.importSelector.selectImports(new StandardAnnotationMetadata(source));
	}

	@EnableAutoConfiguration
	private class BasicEnableAutoConfiguration {

	}

}
