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

package org.springframework.boot.logging.log4j2;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.PropertySource;

/**
 * Spring Boot {@link PropertySource} that disables Log4j2's shutdown hook.
 *
 * @author Andy Wilkinson
 * @since 2.5.2
 */
public class SpringBootPropertySource implements PropertySource {

	private static final String PREFIX = "log4j.";

	private final Map<String, String> properties = Collections
		.singletonMap(ShutdownCallbackRegistry.SHUTDOWN_HOOK_ENABLED, "false");

	/**
	 * Applies the given action to each key-value pair in the properties map.
	 * @param action the action to be applied to each key-value pair
	 * @throws NullPointerException if the specified action is null
	 */
	@Override
	public void forEach(BiConsumer<String, String> action) {
		this.properties.forEach(action::accept);
	}

	/**
	 * Returns the normal form of the given tokens.
	 * @param tokens the tokens to be converted to normal form
	 * @return the normal form of the tokens
	 */
	@Override
	public CharSequence getNormalForm(Iterable<? extends CharSequence> tokens) {
		return PREFIX + Util.joinAsCamelCase(tokens);
	}

	/**
	 * Returns the priority of the SpringBootPropertySource.
	 * @return the priority of the SpringBootPropertySource
	 */
	@Override
	public int getPriority() {
		return -200;
	}

	/**
	 * Retrieves the value of the property associated with the specified key.
	 * @param key the key of the property to retrieve
	 * @return the value of the property, or null if the key is not found
	 */
	@Override
	public String getProperty(String key) {
		return this.properties.get(key);
	}

	/**
	 * Checks if the specified key is present in the properties map.
	 * @param key the key to check
	 * @return true if the key is present, false otherwise
	 */
	@Override
	public boolean containsProperty(String key) {
		return this.properties.containsKey(key);
	}

	/**
	 * Returns a collection of property names.
	 * @return a collection of property names
	 */
	@Override
	public Collection<String> getPropertyNames() {
		return this.properties.keySet();
	}

}
