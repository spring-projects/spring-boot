/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Exception thrown when the application has configured an incompatible set of
 * {@link ConfigurationProperties} keys.
 *
 * @author Brian Clozel
 * @since 2.4.0
 */
public class IncompatibleConfigurationException extends RuntimeException {

	private final List<String> incompatibleKeys;

	public IncompatibleConfigurationException(String... incompatibleKeys) {
		super("The following configuration properties have incompatible values: " + Arrays.toString(incompatibleKeys));
		this.incompatibleKeys = Arrays.asList(incompatibleKeys);
	}

	public Collection<String> getIncompatibleKeys() {
		return this.incompatibleKeys;
	}

}
