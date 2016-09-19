/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Mappings between {@link StoreType} and {@code @Configuration}.
 *
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 */
final class SessionStoreMappings {

	private static final Map<StoreType, Class<?>> MAPPINGS;

	static {
		Map<StoreType, Class<?>> mappings = new HashMap<StoreType, Class<?>>();
		mappings.put(StoreType.REDIS, RedisSessionConfiguration.class);
		mappings.put(StoreType.MONGO, MongoSessionConfiguration.class);
		mappings.put(StoreType.JDBC, JdbcSessionConfiguration.class);
		mappings.put(StoreType.HAZELCAST, HazelcastSessionConfiguration.class);
		mappings.put(StoreType.HASH_MAP, HashMapSessionConfiguration.class);
		mappings.put(StoreType.NONE, NoOpSessionConfiguration.class);
		MAPPINGS = Collections.unmodifiableMap(mappings);
	}

	private SessionStoreMappings() {
	}

	public static String getConfigurationClass(StoreType sessionStoreType) {
		Class<?> configurationClass = MAPPINGS.get(sessionStoreType);
		Assert.state(configurationClass != null,
				"Unknown session store type " + sessionStoreType);
		return configurationClass.getName();
	}

	public static StoreType getType(String configurationClassName) {
		for (Map.Entry<StoreType, Class<?>> entry : MAPPINGS.entrySet()) {
			if (entry.getValue().getName().equals(configurationClassName)) {
				return entry.getKey();
			}
		}
		throw new IllegalStateException(
				"Unknown configuration class " + configurationClassName);
	}

}
