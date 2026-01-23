/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.boot.build;

import java.util.Properties;

import org.gradle.api.Task;
import org.gradle.api.internal.PropertiesTransformer;
import org.gradle.plugins.ide.api.PropertiesGeneratorTask;
import org.gradle.plugins.ide.internal.generator.PropertiesPersistableConfigurationObject;

import org.springframework.boot.build.EclipseSynchronizeJdtSettings.Configuration;

/**
 * {@link Task} to do something.
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

	static class Configuration extends PropertiesPersistableConfigurationObject {

		Configuration(PropertiesTransformer transformer) {
			super(transformer);
		}

		@Override
		protected String getDefaultResourceName() {
			return null;
		}

		@Override
		protected void load(Properties properties) {
		}

		@Override
		protected void store(Properties properties) {
			properties.put("org.eclipse.jdt.core.compiler.release", "true");
		}

	}

}
