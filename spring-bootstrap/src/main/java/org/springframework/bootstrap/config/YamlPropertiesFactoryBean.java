/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.bootstrap.config;

import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;

/**
 * Factory for Java Properties that reads from a YAML source. YAML is a nice
 * human-readable format for configuration, and it has some useful hierarchical
 * properties. It's more or less a superset of JSON, so it has a lot of similar features.
 * The Properties created by this factory have nested paths for hierarchical objects, so
 * for instance this YAML
 * 
 * <pre>
 * environments:
 *   dev:
 *     url: http://dev.bar.com
 *     name: Developer Setup
 *   prod:
 *     url: http://foo.bar.com
 *     name: My Cool App
 * </pre>
 * 
 * is transformed into these Properties:
 * 
 * <pre>
 * environments.dev.url=http://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=http://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 * 
 * Lists are represented as comma-separated values (useful for simple String values) and
 * also as property keys with <code>[]</code> dereferencers, for example this YAML:
 * 
 * <pre>
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 * 
 * becomes java Properties like this:
 * 
 * <pre>
 * servers=dev.bar.com,foo.bar.com
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 * 
 * @author Dave Syer
 * @since 3.2
 * 
 */
public class YamlPropertiesFactoryBean extends YamlProcessor implements
		FactoryBean<Properties> {

	private boolean singleton = true;

	private Properties instance;

	@Override
	public Properties getObject() {
		if (!this.singleton || this.instance == null) {
			final Properties result = new Properties();
			MatchCallback callback = new MatchCallback() {
				@Override
				public void process(Properties properties, Map<String, Object> map) {
					result.putAll(properties);
				}
			};
			process(callback);
			this.instance = result;
		}
		return this.instance;
	}

	@Override
	public Class<?> getObjectType() {
		return Properties.class;
	}

	/**
	 * Set if a singleton should be created, or a new object on each request otherwise.
	 * Default is <code>true</code> (a singleton).
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}
}
