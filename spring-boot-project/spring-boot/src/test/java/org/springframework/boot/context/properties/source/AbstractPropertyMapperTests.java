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

package org.springframework.boot.context.properties.source;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Abstract base class for {@link PropertyMapper} tests.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Eddú Meléndez
 */
public abstract class AbstractPropertyMapperTests {

	protected abstract PropertyMapper getMapper();

	protected final Iterator<String> namesFromString(String name) {
		return namesFromString(name, "value");
	}

	protected final Iterator<String> namesFromString(String name, Object value) {
		return Arrays.stream(getMapper().map(name))
				.map((mapping) -> mapping.getConfigurationPropertyName().toString())
				.iterator();
	}

	protected final Iterator<String> namesFromConfiguration(String name) {
		return namesFromConfiguration(name, "value");
	}

	protected final Iterator<String> namesFromConfiguration(String name, String value) {
		return Arrays.stream(getMapper().map(ConfigurationPropertyName.of(name)))
				.map(PropertyMapping::getPropertySourceName).iterator();
	}

}
