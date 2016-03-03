/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.info;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.validation.BindException;

/**
 * A base {@link InfoContributor} implementation working on the {@link Environment}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public abstract class AbstractEnvironmentInfoContributor implements InfoContributor {

	private final ConfigurableEnvironment environment;

	protected AbstractEnvironmentInfoContributor(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	public final ConfigurableEnvironment getEnvironment() {
		return this.environment;
	}

	/**
	 * Extract the keys from the environment using the specified {@code prefix}. The
	 * prefix won't be included.
	 * <p>
	 * Any key that starts with the {@code prefix} will be included
	 * @param prefix the prefix to use
	 * @return the keys from the environment matching the prefix
	 */
	protected Map<String, Object> extract(String prefix) {
		Map<String, Object> content = new LinkedHashMap<String, Object>();
		bindEnvironmentTo(prefix, content);
		return content;
	}

	/**
	 * Bind the specified {@code target} from the environment using the {@code prefix}.
	 * <p>
	 * Any key that starts with the {@code prefix} will be bound to the {@code target}.
	 * @param prefix the prefix to use
	 * @param target the object to bind to
	 */
	protected void bindEnvironmentTo(String prefix, Object target) {
		PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
				target);
		factory.setTargetName(prefix);
		factory.setPropertySources(this.environment.getPropertySources());
		try {
			factory.bindPropertiesToTarget();
		}
		catch (BindException ex) {
			throw new IllegalStateException("Cannot bind to " + target, ex);
		}
	}

}
