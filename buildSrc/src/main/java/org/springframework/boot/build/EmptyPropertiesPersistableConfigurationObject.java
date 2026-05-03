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
