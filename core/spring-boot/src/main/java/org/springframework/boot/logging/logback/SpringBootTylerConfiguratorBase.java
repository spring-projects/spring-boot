/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;

/**
 * Base class of the Logback configurator that is generated, during AOT processing, from
 * the application's Logback XML configuration. Provides the Spring-specific support
 * required by {@code <springProperty>} and {@code <springProfile>} elements, resolved
 * against the {@link Environment} available at runtime.
 * <p>
 * This class is intended only for use by generated code.
 *
 * @author Ceki Gülcü
 * @since 4.2.0
 */
public class SpringBootTylerConfiguratorBase extends TylerConfiguratorBase {

	/**
	 * Name of the configurator class generated during AOT processing.
	 */
	static final String GENERATED_CLASS_NAME = "org.springframework.boot.logging.logback.aot.GeneratedLogbackConfigurator";

	/**
	 * Return the value of the given {@link Environment} property, as for a
	 * {@code <springProperty>} element.
	 * @param source the name of the property in the environment
	 * @param defaultValue the value to use if the property is not set
	 * @return the value of the property or the default value
	 */
	protected @Nullable String springProperty(String source, @Nullable String defaultValue) {
		Environment environment = getEnvironment();
		if (environment == null) {
			addWarn("No Spring Environment available to resolve " + source);
			return defaultValue;
		}
		return (defaultValue != null) ? environment.getProperty(source, defaultValue) : environment.getProperty(source);
	}

	/**
	 * Return whether the {@link Environment} accepts the given profiles, as for a
	 * {@code <springProfile>} element.
	 * @param names the comma-separated profile names or expressions, as specified in the
	 * {@code name} attribute
	 * @return whether the profiles are accepted
	 */
	protected boolean acceptsProfiles(String names) {
		return SpringProfileModelHandler.acceptsProfiles(getEnvironment(), names, this, getContext());
	}

	private @Nullable Environment getEnvironment() {
		return (Environment) getContext().getObject(Environment.class.getName());
	}

}
