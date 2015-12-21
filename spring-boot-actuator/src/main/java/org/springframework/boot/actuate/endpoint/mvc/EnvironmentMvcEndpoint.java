/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Adapter to expose {@link EnvironmentEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 */
public class EnvironmentMvcEndpoint extends EndpointMvcAdapter
		implements EnvironmentAware {

	private Environment environment;

	public EnvironmentMvcEndpoint(EnvironmentEndpoint delegate) {
		super(delegate);
	}

	@RequestMapping(value = "/{name:.*}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@HypermediaDisabled
	public Object value(@PathVariable String name) {
		if (!getDelegate().isEnabled()) {
			// Shouldn't happen - MVC endpoint shouldn't be registered when delegate's
			// disabled
			return getDisabledResponse();
		}
		return new NamePatternEnvironmentFilter(this.environment).getResults(name);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * {@link NamePatternFilter} for the Environment source.
	 */
	private class NamePatternEnvironmentFilter extends NamePatternFilter<Environment> {

		NamePatternEnvironmentFilter(Environment source) {
			super(source);
		}

		@Override
		protected void getNames(Environment source, NameCallback callback) {
			if (source instanceof ConfigurableEnvironment) {
				getNames(((ConfigurableEnvironment) source).getPropertySources(),
						callback);
			}
		}

		private void getNames(PropertySources propertySources, NameCallback callback) {
			for (PropertySource<?> propertySource : propertySources) {
				if (propertySource instanceof EnumerablePropertySource) {
					EnumerablePropertySource<?> source = (EnumerablePropertySource<?>) propertySource;
					for (String name : source.getPropertyNames()) {
						callback.addName(name);
					}
				}
			}
		}

		@Override
		protected Object getValue(Environment source, String name) {
			String result = source.getProperty(name);
			if (result == null) {
				throw new NoSuchPropertyException("No such property: " + name);
			}
			return ((EnvironmentEndpoint) getDelegate()).sanitize(name, result);
		}

	}

	/**
	 * Exception thrown when the specified property cannot be found.
	 */
	@SuppressWarnings("serial")
	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such property")
	public static class NoSuchPropertyException extends RuntimeException {

		public NoSuchPropertyException(String string) {
			super(string);
		}

	}

}
