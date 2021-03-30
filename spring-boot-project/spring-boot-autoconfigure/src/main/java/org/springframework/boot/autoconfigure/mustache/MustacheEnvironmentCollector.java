/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import java.util.concurrent.atomic.AtomicBoolean;

import com.samskivert.mustache.DefaultCollector;
import com.samskivert.mustache.Mustache.Collector;
import com.samskivert.mustache.Mustache.VariableFetcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Mustache {@link Collector} to expose properties from the Spring {@link Environment}.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 * @since 1.2.2
 * @deprecated since 2.5.0 for removal in 2.7.0 in favor of direct addition of values from
 * the Environment to the model
 */
@Deprecated
public class MustacheEnvironmentCollector extends DefaultCollector implements EnvironmentAware {

	private ConfigurableEnvironment environment;

	private final VariableFetcher propertyFetcher = new PropertyVariableFetcher();

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	@Override
	public VariableFetcher createFetcher(Object ctx, String name) {
		VariableFetcher fetcher = super.createFetcher(ctx, name);
		if (fetcher != null) {
			return fetcher;
		}
		if (this.environment.containsProperty(name)) {
			return this.propertyFetcher;
		}
		return null;
	}

	private class PropertyVariableFetcher implements VariableFetcher {

		private final Log log = LogFactory.getLog(PropertyVariableFetcher.class);

		private final AtomicBoolean logDeprecationWarning = new AtomicBoolean();

		@Override
		public Object get(Object ctx, String name) {
			String property = MustacheEnvironmentCollector.this.environment.getProperty(name);
			if (property != null && this.logDeprecationWarning.compareAndSet(false, true)) {
				this.log.warn("Mustache variable resolution relied upon deprecated support for falling back to the "
						+ "Spring Environment. Please add values from the Environment directly to the model instead.");
			}
			return property;
		}

	}

}
