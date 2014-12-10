/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Abstract base class for endpoint tests.
 *
 * @author Phillip Webb
 */
public abstract class AbstractEndpointTests<T extends Endpoint<?>> {

	protected AnnotationConfigApplicationContext context;

	private final Class<?> configClass;

	private final Class<?> type;

	private final String id;

	private final boolean sensitive;

	private final String property;

	public AbstractEndpointTests(Class<?> configClass, Class<?> type, String id,
			boolean sensitive, String property) {
		this.configClass = configClass;
		this.type = type;
		this.id = id;
		this.sensitive = sensitive;
		this.property = property;
	}

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(this.configClass);
		this.context.refresh();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void getId() throws Exception {
		assertThat(getEndpointBean().getId(), equalTo(this.id));
	}

	@Test
	public void isSensitive() throws Exception {
		assertThat(getEndpointBean().isSensitive(), equalTo(this.sensitive));
	}

	@Test
	public void idOverride() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, this.property + ".id:myid");
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().getId(), equalTo("myid"));
	}

	@Test
	public void isSensitiveOverride() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		PropertySource<?> propertySource = new MapPropertySource("test",
				Collections.<String, Object> singletonMap(this.property + ".sensitive",
						String.valueOf(!this.sensitive)));
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().isSensitive(), equalTo(!this.sensitive));
	}

	@Test
	public void isEnabledByDefault() throws Exception {
		assertThat(getEndpointBean().isEnabled(), equalTo(true));
	}

	@Test
	public void isEnabledFallbackToEnvironment() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		PropertySource<?> propertySource = new MapPropertySource("test",
				Collections.<String, Object> singletonMap(this.property + ".enabled",
						false));
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().isEnabled(), equalTo(false));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void isExplicitlyEnabled() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		PropertySource<?> propertySource = new MapPropertySource("test",
				Collections.<String, Object> singletonMap(this.property + ".enabled",
						false));
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		((AbstractEndpoint) getEndpointBean()).setEnabled(true);
		assertThat(getEndpointBean().isEnabled(), equalTo(true));
	}

	@SuppressWarnings("unchecked")
	protected T getEndpointBean() {
		return (T) this.context.getBean(this.type);
	}

}
