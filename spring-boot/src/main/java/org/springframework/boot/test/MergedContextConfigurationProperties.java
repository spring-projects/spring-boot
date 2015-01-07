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

package org.springframework.boot.test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Provides access to {@link MergedContextConfiguration} properties.
 *
 * @author Phillip Webb
 * @since 1.2.1
 */
class MergedContextConfigurationProperties {

	private final MergedContextConfiguration configuration;

	public MergedContextConfigurationProperties(MergedContextConfiguration configuration) {
		this.configuration = configuration;
	}

	public void add(String[] properties) {
		Set<String> merged = new LinkedHashSet<String>((Arrays.asList(this.configuration
				.getPropertySourceProperties())));
		merged.addAll(Arrays.asList(properties));
		addIntegrationTestProperty(merged);
		ReflectionTestUtils.setField(this.configuration, "propertySourceProperties",
				merged.toArray(new String[merged.size()]));
	}

	/**
	 * Add an "IntegrationTest" property to ensure that there is something to
	 * differentiate regular tests and {@code @IntegrationTest} tests. Without this
	 * property a cached context could be returned that hadn't started the embedded
	 * servlet container.
	 * @param propertySourceProperties the property source properties
	 */
	private void addIntegrationTestProperty(Set<String> propertySourceProperties) {
		propertySourceProperties.add(IntegrationTest.class.getName() + "=true");
	}

}
