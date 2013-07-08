/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.context.embedded;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * {@link EmbeddedWebApplicationContext} which takes its configuration from XML documents,
 * understood by an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 * 
 * <p>
 * Note: In case of multiple config locations, later bean definitions will override ones
 * defined in earlier loaded files. This can be leveraged to deliberately override certain
 * bean definitions via an extra XML file.
 * 
 * @author Phillip Webb
 * @see #setNamespace
 * @see #setConfigLocations
 * @see EmbeddedWebApplicationContext
 * @see XmlWebApplicationContext
 */
public class XmlEmbeddedWebApplicationContext extends EmbeddedWebApplicationContext {

	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

	/**
	 * Create a new {@link XmlEmbeddedWebApplicationContext} that needs to be
	 * {@linkplain #load loaded} and then manually {@link #refresh refreshed}.
	 */
	public XmlEmbeddedWebApplicationContext() {
		this.reader.setEnvironment(this.getEnvironment());
	}

	/**
	 * Create a new {@link XmlEmbeddedWebApplicationContext}, loading bean definitions
	 * from the given resources and automatically refreshing the context.
	 * @param resources the resources to load from
	 */
	public XmlEmbeddedWebApplicationContext(Resource... resources) {
		load(resources);
		refresh();
	}

	/**
	 * Create a new {@link XmlEmbeddedWebApplicationContext}, loading bean definitions
	 * from the given resource locations and automatically refreshing the context.
	 * @param resourceLocations the resources to load from
	 */
	public XmlEmbeddedWebApplicationContext(String... resourceLocations) {
		load(resourceLocations);
		refresh();
	}

	/**
	 * Create a new {@link XmlEmbeddedWebApplicationContext}, loading bean definitions
	 * from the given resource locations and automatically refreshing the context.
	 * @param relativeClass class whose package will be used as a prefix when loading each
	 * specified resource name
	 * @param resourceNames relatively-qualified names of resources to load
	 */
	public XmlEmbeddedWebApplicationContext(Class<?> relativeClass,
			String... resourceNames) {
		load(relativeClass, resourceNames);
		refresh();
	}

	/**
	 * Set whether to use XML validation. Default is {@code true}.
	 */
	public void setValidating(boolean validating) {
		this.reader.setValidating(validating);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Delegates the given environment to underlying {@link XmlBeanDefinitionReader}.
	 * Should be called before any call to {@link #load}.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(this.getEnvironment());
	}

	/**
	 * Load bean definitions from the given XML resources.
	 * @param resources one or more resources to load from
	 */
	public final void load(Resource... resources) {
		this.reader.loadBeanDefinitions(resources);
	}

	/**
	 * Load bean definitions from the given XML resources.
	 * @param resourceLocations one or more resource locations to load from
	 */
	public final void load(String... resourceLocations) {
		this.reader.loadBeanDefinitions(resourceLocations);
	}

	/**
	 * Load bean definitions from the given XML resources.
	 * @param relativeClass class whose package will be used as a prefix when loading each
	 * specified resource name
	 * @param resourceNames relatively-qualified names of resources to load
	 */
	public final void load(Class<?> relativeClass, String... resourceNames) {
		Resource[] resources = new Resource[resourceNames.length];
		for (int i = 0; i < resourceNames.length; i++) {
			resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
		}
		this.reader.loadBeanDefinitions(resources);
	}

	@Override
	public final void refresh() throws BeansException, IllegalStateException {
		super.refresh();
	}
}
