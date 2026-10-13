/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build;

import java.util.Properties;

import org.gradle.api.Task;
import org.gradle.api.internal.PropertiesTransformer;
import org.gradle.plugins.ide.api.PropertiesGeneratorTask;

import org.springframework.boot.build.EclipseSynchronizeJdtSettings.Configuration;

/**
 * {@link Task} to synchronize Eclipse JDT settings.
 *
 * @author Phillip Webb
 */
public abstract class EclipseSynchronizeJdtSettings extends PropertiesGeneratorTask<Configuration> {

	@Override
	protected Configuration create() {
		return new Configuration(getTransformer());
	}

	@Override
	protected void configure(Configuration configuration) {
	}

	static class Configuration extends EmptyPropertiesPersistableConfigurationObject {

		Configuration(PropertiesTransformer transformer) {
			super(transformer);
		}

		@Override
		protected void store(Properties properties) {
			properties.put("org.eclipse.jdt.core.compiler.release", "true");
		}

	}

}
