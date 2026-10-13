/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Function used to determine if a {@link ConfigurationPropertySource} should be included
 * when determining unbound elements. If the underlying {@link PropertySource} is a
 * systemEnvironment or systemProperties property source, it will not be considered for
 * unbound element failures.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class UnboundElementsSourceFilter implements Function<ConfigurationPropertySource, Boolean> {

	private static final Set<String> BENIGN_PROPERTY_SOURCE_NAMES = Collections
		.unmodifiableSet(new HashSet<>(Arrays.asList(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)));

	@Override
	public Boolean apply(ConfigurationPropertySource configurationPropertySource) {
		Object underlyingSource = configurationPropertySource.getUnderlyingSource();
		if (underlyingSource instanceof PropertySource<?> propertySource) {
			String name = propertySource.getName();
			return !BENIGN_PROPERTY_SOURCE_NAMES.contains(name);
		}
		return true;
	}

}
