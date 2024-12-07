/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.context;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An implementation of {@link PropertySourceFactory} that delegates the loading of
 * {@code PropertySource} to {@link YamlPropertySourceLoader}.
 * <p>
 * Even though {@link YamlPropertySourceLoader} supports multi-document YAML files, the
 * {@code YamlPropertySourceFactory} intentionally does not allow this.
 *
 * @author Dmytro Nosan
 * @since 3.5.0
 * @see TestYamlPropertySource
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

	private static final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) throws IOException {
		Resource resource = encodedResource.getResource();
		String propertySourceName = getPropertySourceName(name, resource);
		List<PropertySource<?>> propertySources = loader.load(propertySourceName, resource);
		Assert.isTrue(propertySources.size() <= 1, () -> resource + " is a multi-document YAML file");
		if (propertySources.isEmpty()) {
			return new MapPropertySource(name, Collections.emptyMap());
		}
		return propertySources.get(0);
	}

	private static String getPropertySourceName(String name, Resource resource) {
		if (StringUtils.hasText(name)) {
			return name;
		}
		String description = resource.getDescription();
		if (StringUtils.hasText(description)) {
			return description;
		}
		return resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
	}

}
