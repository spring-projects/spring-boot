/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.List;

/**
 * Abstract base class for {@link PropertyMapper} tests.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Eddú Meléndez
 */
public abstract class AbstractPropertyMapperTests {

	protected abstract PropertyMapper getMapper();

	protected final List<String> mapConfigurationPropertyName(String configurationPropertyName) {
		return getMapper().map(ConfigurationPropertyName.of(configurationPropertyName));
	}

	protected final String mapPropertySourceName(String propertySourceName) {
		return getMapper().map(propertySourceName).toString();
	}

}
