/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.json;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import org.springframework.util.Assert;

/**
 * Thin wrapper to adapt Snake {@link Yaml} to {@link JsonParser}.
 *
 * @author Dave Syer
 * @author Jean de Klerk
 * @since 1.0.0
 * @see JsonParserFactory
 */
public class YamlJsonParser extends AbstractJsonParser {

	private final Yaml yaml = new Yaml(new TypeLimitedConstructor());

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> parseMap(String json) {
		return parseMap(json, (trimmed) -> this.yaml.loadAs(trimmed, Map.class));
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object> parseList(String json) {
		return parseList(json, (trimmed) -> this.yaml.loadAs(trimmed, List.class));
	}

	private static class TypeLimitedConstructor extends Constructor {

		private static final Set<String> SUPPORTED_TYPES;
		static {
			Set<Class<?>> supportedTypes = new LinkedHashSet<>();
			supportedTypes.add(List.class);
			supportedTypes.add(Map.class);
			SUPPORTED_TYPES = supportedTypes.stream()
				.map(Class::getName)
				.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
		}

		@Override
		protected Class<?> getClassForName(String name) throws ClassNotFoundException {
			Assert.state(SUPPORTED_TYPES.contains(name),
					() -> "Unsupported '" + name + "' type encountered in YAML document");
			return super.getClassForName(name);
		}

	}

}
