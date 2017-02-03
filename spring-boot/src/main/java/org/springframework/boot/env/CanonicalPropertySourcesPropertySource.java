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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

/**
 * Adapter that exposes a list of {@link CanonicalPropertySource} instances as an
 * {@link EnumerablePropertySource} so that they can be used with a
 * {@link PropertyResolver} or added to the {@link Environment}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class CanonicalPropertySourcesPropertySource
		extends EnumerablePropertySource<List<CanonicalPropertySource>>
		implements OriginCapablePropertySource {

	public CanonicalPropertySourcesPropertySource(String name,
			List<CanonicalPropertySource> source) {
		super(name, source);
	}

	@Override
	public Object getProperty(String name) {
		CanonicalPropertyValue value = getCanonicalProperty(name);
		return (value == null ? null : value.getValue());
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> names = new LinkedHashSet<>();
		for (CanonicalPropertySource source : getSource()) {
			source.getCanonicalPropertyNames().stream().map(Object::toString)
					.collect(Collectors.toCollection(() -> names));
		}
		return names.toArray(new String[names.size()]);
	}

	@Override
	public PropertyOrigin getPropertyOrigin(String name) {
		CanonicalPropertyValue value = getCanonicalProperty(name);
		return (value == null ? null : value.getOrigin());

	}

	private CanonicalPropertyValue getCanonicalProperty(String name) {
		if (!CanonicalPropertyName.isValid(name)) {
			return null;
		}
		CanonicalPropertyName canonicalName = new CanonicalPropertyName(name);
		return getSource().stream().map(source -> source.getProperty(canonicalName))
				.filter(Objects::nonNull).findFirst().orElse(null);
	}

}
