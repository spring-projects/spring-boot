/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.util.PropertySource;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;

/**
 * Returns properties from Spring.
 *
 * @author Ralph Goers
 */
class SpringEnvironmentPropertySource implements PropertySource {

	/**
	 * System properties take precedence followed by properties in Log4j properties files.
	 */
	private static final int PRIORITY = -100;

	private volatile @Nullable Environment environment;

	@Override
	public int getPriority() {
		return PRIORITY;
	}

	@Override
	public @Nullable String getProperty(String key) {
		Environment environment = this.environment;
		return (environment != null) ? environment.getProperty(key) : null;
	}

	@Override
	public boolean containsProperty(String key) {
		Environment environment = this.environment;
		return environment != null && environment.containsProperty(key);
	}

	void setEnvironment(@Nullable Environment environment) {
		this.environment = environment;
	}

}
