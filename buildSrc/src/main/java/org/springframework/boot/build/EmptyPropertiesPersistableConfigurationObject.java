/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.gradle.api.internal.PropertiesTransformer;
import org.gradle.internal.UncheckedException;
import org.gradle.plugins.ide.internal.generator.PropertiesPersistableConfigurationObject;

/**
 * Base class for {@link PropertiesPersistableConfigurationObject} instances start empty
 * and have no default resource.
 *
 * @author Phillip Webb
 */
abstract class EmptyPropertiesPersistableConfigurationObject extends PropertiesPersistableConfigurationObject {

	EmptyPropertiesPersistableConfigurationObject(PropertiesTransformer transformer) {
		super(transformer);
	}

	@Override
	protected String getDefaultResourceName() {
		return null;
	}

	@Override
	public void loadDefaults() {
		try {
			load(new ByteArrayInputStream(new byte[0]));
		}
		catch (Exception ex) {
			throw UncheckedException.throwAsUncheckedException(ex);
		}
	}

	@Override
	protected void load(Properties properties) {
	}

}
