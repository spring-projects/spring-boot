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

package org.springframework.boot.env;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ObjectUtils;

/**
 * A {@link CanonicalPropertySource} backed by a Spring {@link PropertySource}.
 *
 * @author Phillip Webb
 */
public class PropertySourceCanonicalPropertySource implements CanonicalPropertySource {

	private final EnumerablePropertySource<?> source;

	private Function<String, CanonicalPropertyName> nameMapper;

	public PropertySourceCanonicalPropertySource(EnumerablePropertySource<?> source) {
		this.source = source;
	}

	@Override
	public CanonicalPropertyValue getProperty(CanonicalPropertyName name) {
		String sourceName = findSourceName(name);
		if (this.source.containsProperty(sourceName)) {
			Object sourceValue = this.source.getProperty(sourceName);
			return adapt(sourceName, sourceValue);
		}
		return null;
	}

	private CanonicalPropertyValue adapt(String sourceName, Object sourceValue) {
		if (this.source instanceof OriginCapablePropertySource) {
			// FIXME get the origin
		}
		return null;
	}

	@Override
	public Set<CanonicalPropertyName> getCanonicalPropertyNames() {
		return Arrays.stream(this.source.getPropertyNames()).map(this.nameMapper)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private String findSourceName(CanonicalPropertyName name) {
		for (String candidate : this.source.getPropertyNames()) {
			if (ObjectUtils.nullSafeEquals(name, this.nameMapper.apply(candidate))) {
				return candidate;
			}
		}
		return null;
	}

}
