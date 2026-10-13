/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.PropertySource;
import org.jspecify.annotations.Nullable;

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

	@Override
	public void forEach(BiConsumer<String, String> action) {
		this.properties.forEach(action);
	}

	@Override
	public CharSequence getNormalForm(Iterable<? extends CharSequence> tokens) {
		return PREFIX + Util.joinAsCamelCase(tokens);
	}

	@Override
	public int getPriority() {
		return -200;
	}

	@Override
	public @Nullable String getProperty(String key) {
		return this.properties.get(key);
	}

	@Override
	public boolean containsProperty(String key) {
		return this.properties.containsKey(key);
	}

	@Override
	public Collection<String> getPropertyNames() {
		return this.properties.keySet();
	}

}
