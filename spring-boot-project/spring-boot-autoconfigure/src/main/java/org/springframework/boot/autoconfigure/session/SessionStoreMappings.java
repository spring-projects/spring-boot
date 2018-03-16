/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.WebApplicationType;
import org.springframework.util.Assert;

/**
 * Mappings between {@link StoreType} and {@code @Configuration}.
 *
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 */
final class SessionStoreMappings {

	private static final Map<StoreType, Map<WebApplicationType, Class<?>>> MAPPINGS;

	static {
		Map<StoreType, Map<WebApplicationType, Class<?>>> mappings = new EnumMap<>(
				StoreType.class);
		mappings.put(StoreType.REDIS, createMapping(RedisSessionConfiguration.class,
				RedisReactiveSessionConfiguration.class));
		mappings.put(StoreType.MONGODB, createMapping(MongoSessionConfiguration.class,
				MongoReactiveSessionConfiguration.class));
		mappings.put(StoreType.JDBC, createMapping(JdbcSessionConfiguration.class));
		mappings.put(StoreType.HAZELCAST,
				createMapping(HazelcastSessionConfiguration.class));
		mappings.put(StoreType.NONE, createMapping(NoOpSessionConfiguration.class,
				NoOpReactiveSessionConfiguration.class));
		MAPPINGS = Collections.unmodifiableMap(mappings);
	}

	static Map<WebApplicationType, Class<?>> createMapping(
			Class<?> servletConfiguration) {
		return createMapping(servletConfiguration, null);
	}

	static Map<WebApplicationType, Class<?>> createMapping(Class<?> servletConfiguration,
			Class<?> reactiveConfiguration) {
		Map<WebApplicationType, Class<?>> mapping = new EnumMap<>(
				WebApplicationType.class);
		mapping.put(WebApplicationType.SERVLET, servletConfiguration);
		if (reactiveConfiguration != null) {
			mapping.put(WebApplicationType.REACTIVE, reactiveConfiguration);
		}
		return mapping;
	}

	private SessionStoreMappings() {
	}

	static String getConfigurationClass(WebApplicationType webApplicationType,
			StoreType sessionStoreType) {
		Map<WebApplicationType, Class<?>> configurationClasses = MAPPINGS
				.get(sessionStoreType);
		Assert.state(configurationClasses != null,
				() -> "Unknown session store type " + sessionStoreType);
		Class<?> configurationClass = configurationClasses.get(webApplicationType);
		if (configurationClass == null) {
			return null;
		}
		return configurationClass.getName();
	}

	static StoreType getType(WebApplicationType webApplicationType,
			String configurationClassName) {
		for (Map.Entry<StoreType, Map<WebApplicationType, Class<?>>> storeEntry : MAPPINGS
				.entrySet()) {
			for (Map.Entry<WebApplicationType, Class<?>> entry : storeEntry.getValue()
					.entrySet()) {
				if (entry.getKey() == webApplicationType
						&& entry.getValue().getName().equals(configurationClassName)) {
					return storeEntry.getKey();
				}
			}
		}
		throw new IllegalStateException(
				"Unknown configuration class " + configurationClassName);
	}

}
