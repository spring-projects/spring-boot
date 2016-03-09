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

package org.springframework.boot.autoconfigure.mustache;

import java.util.HashMap;
import java.util.Map;

import com.samskivert.mustache.DefaultCollector;
import com.samskivert.mustache.Mustache.Collector;
import com.samskivert.mustache.Mustache.VariableFetcher;

import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Mustache {@link Collector} to expose properties from the Spring {@link Environment}.
 *
 * @author Dave Syer
 * @since 1.2.2
 */
public class MustacheEnvironmentCollector extends DefaultCollector
		implements EnvironmentAware {

	private ConfigurableEnvironment environment;

	private Map<String, Object> target;

	private RelaxedPropertyResolver propertyResolver;

	private final VariableFetcher propertyFetcher = new PropertyVariableFetcher();

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
		this.target = new HashMap<String, Object>();
		new RelaxedDataBinder(this.target).bind(
				new PropertySourcesPropertyValues(this.environment.getPropertySources()));
		this.propertyResolver = new RelaxedPropertyResolver(environment);
	}

	@Override
	public VariableFetcher createFetcher(Object ctx, String name) {
		VariableFetcher fetcher = super.createFetcher(ctx, name);
		if (fetcher != null) {
			return fetcher;
		}
		if (this.propertyResolver.containsProperty(name)) {
			return this.propertyFetcher;
		}
		return null;
	}

	private class PropertyVariableFetcher implements VariableFetcher {

		@Override
		public Object get(Object ctx, String name) throws Exception {
			return MustacheEnvironmentCollector.this.propertyResolver.getProperty(name);
		}

	}

}
