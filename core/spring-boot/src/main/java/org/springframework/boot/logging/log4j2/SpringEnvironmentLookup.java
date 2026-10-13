/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerContextAware;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Lookup for Spring properties.
 *
 * @author Ralph Goers
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
@Plugin(name = "spring", category = StrLookup.CATEGORY)
class SpringEnvironmentLookup implements LoggerContextAware, StrLookup {

	private volatile @Nullable Environment environment;

	@Override
	public @Nullable String lookup(LogEvent event, String key) {
		return lookup(key);
	}

	@Override
	public @Nullable String lookup(String key) {
		Environment environment = this.environment;
		Assert.state(environment != null,
				"Unable to obtain Spring Environment from LoggerContext. "
						+ "This can happen if your log4j2 configuration filename does not end with '-spring' "
						+ "(for example using 'log4j2.xml' instead of 'log4j2-spring.xml')");
		return environment.getProperty(key);
	}

	@Override
	public void setLoggerContext(LoggerContext loggerContext) {
		this.environment = Log4J2LoggingSystem.getEnvironment(loggerContext);
	}

}
