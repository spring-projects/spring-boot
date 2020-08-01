/*
 * Copyright 2012-2020 the original author or authors.
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

import com.samskivert.mustache.DefaultCollector;
import com.samskivert.mustache.Mustache.Collector;
import com.samskivert.mustache.Mustache.VariableFetcher;
import com.samskivert.mustache.Template;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Mustache {@link Collector} to expose properties from the Spring {@link Environment}.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 * @since 1.2.2
 */
public class MustacheEnvironmentCollector extends DefaultCollector implements EnvironmentAware {

	private ConfigurableEnvironment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	@Override
	public VariableFetcher createFetcher(Object ctx, String name) {
		VariableFetcher nativeFetcher = super.createFetcher(ctx, name);
		if (nativeFetcher != null) {
			return new PropertyVariableFetcher(nativeFetcher);
		}
		if (this.environment.containsProperty(name)) {
			return new PropertyVariableFetcher();
		}
		return null;
	}

	/**
	 * {@link VariableFetcher} that also checks the {@link Environment}.
	 */
	private class PropertyVariableFetcher implements VariableFetcher {

		private final VariableFetcher nativeFetcher;

		PropertyVariableFetcher() {
			this.nativeFetcher = null;
		}

		PropertyVariableFetcher(VariableFetcher delegate) {
			this.nativeFetcher = delegate;
		}

		@Override
		public Object get(Object ctx, String name) {
			Object result = getFromNativeFetcher(ctx, name);
			result = (result != null) ? result : getFromEnvironment(name);
			return (result != null) ? result : Template.NO_FETCHER_FOUND;
		}

		private Object getFromNativeFetcher(Object ctx, String name) {
			try {
				Object result = (this.nativeFetcher != null) ? this.nativeFetcher.get(ctx, name) : null;
				return (result != Template.NO_FETCHER_FOUND) ? result : null;
			}
			catch (Exception ex) {
				return null;
			}
		}

		private Object getFromEnvironment(String name) {
			return MustacheEnvironmentCollector.this.environment.getProperty(name);
		}

	}

}
