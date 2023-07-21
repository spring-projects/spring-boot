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

package org.springframework.boot.testcontainers.properties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.testcontainers.containers.Container;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnumerablePropertySource} backed by a map with values supplied from one or more
 * {@link Container testcontainers}.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
public class TestcontainersPropertySource extends EnumerablePropertySource<Map<String, Supplier<Object>>> {

	static final String NAME = "testcontainersPropertySource";

	private final DynamicPropertyRegistry registry;

	TestcontainersPropertySource() {
		this(Collections.synchronizedMap(new LinkedHashMap<>()));
	}

	private TestcontainersPropertySource(Map<String, Supplier<Object>> valueSuppliers) {
		super(NAME, Collections.unmodifiableMap(valueSuppliers));
		this.registry = (name, valueSupplier) -> {
			Assert.hasText(name, "'name' must not be null or blank");
			Assert.notNull(valueSupplier, "'valueSupplier' must not be null");
			valueSuppliers.put(name, valueSupplier);
		};
	}

	@Override
	public Object getProperty(String name) {
		Supplier<Object> valueSupplier = this.source.get(name);
		return (valueSupplier != null) ? valueSupplier.get() : null;
	}

	@Override
	public boolean containsProperty(String name) {
		return this.source.containsKey(name);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.keySet());
	}

	public static DynamicPropertyRegistry attach(Environment environment) {
		Assert.state(environment instanceof ConfigurableEnvironment,
				"TestcontainersPropertySource can only be attached to a ConfigurableEnvironment");
		return attach((ConfigurableEnvironment) environment);
	}

	private static DynamicPropertyRegistry attach(ConfigurableEnvironment environment) {
		PropertySource<?> propertySource = environment.getPropertySources().get(NAME);
		if (propertySource == null) {
			environment.getPropertySources().addFirst(new TestcontainersPropertySource());
			return attach(environment);
		}
		Assert.state(propertySource instanceof TestcontainersPropertySource,
				"Incorrect DynamicValuesPropertySource type registered");
		return ((TestcontainersPropertySource) propertySource).registry;
	}

}
