/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.env;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * {@link SystemEnvironmentPropertySource} for the content of a {@code .env} file. The
 * variables of a {@code .env} file are environment variables, so they can be accessed and
 * bound in the same relaxed way as the real ones, for example
 * {@code SPRING_DATASOURCE_URL} is {@code spring.datasource.url}. Each value tracks the
 * {@link Origin} where it was defined.
 *
 * @author Sharang Gupta
 */
final class DotEnvPropertySource extends SystemEnvironmentPropertySource
		implements PropertySourceInfo, OriginLookup<String> {

	/**
	 * Suffix that makes the binder map the names of the source like the names of the
	 * system environment.
	 */
	private static final String NAME_SUFFIX = "-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

	DotEnvPropertySource(String name, Map<String, Object> source) {
		super(name + NAME_SUFFIX, source);
	}

	@Override
	public @Nullable Object getProperty(String name) {
		Object value = super.getProperty(name);
		if (value instanceof OriginTrackedValue originTrackedValue) {
			return originTrackedValue.getValue();
		}
		return value;
	}

	@Override
	public @Nullable Origin getOrigin(String name) {
		Object value = getSource().get(resolvePropertyName(name));
		if (value instanceof OriginTrackedValue originTrackedValue) {
			return originTrackedValue.getOrigin();
		}
		return null;
	}

	@Override
	public boolean isImmutable() {
		return true;
	}

}
