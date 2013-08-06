/*
 * Copyright 2012-2013 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.TestUtils;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.MediaType;

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

	private final String path;

	private final boolean sensitive;

	private final String property;

	private MediaType[] produces;

	public AbstractEndpointTests(Class<?> configClass, Class<?> type, String path,
			boolean sensitive, String property, MediaType... produces) {
		this.configClass = configClass;
		this.type = type;
		this.path = path;
		this.sensitive = sensitive;
		this.property = property;
		this.produces = produces;
	}

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(this.configClass);
		this.context.refresh();
	}

	@Test
	public void producesMediaType() {
		assertThat(getEndpointBean().getProduces(), equalTo(this.produces));
	}

	@Test
	public void getPath() throws Exception {
		assertThat(getEndpointBean().getPath(), equalTo(this.path));
	}

	@Test
	public void isSensitive() throws Exception {
		assertThat(getEndpointBean().isSensitive(), equalTo(this.sensitive));
	}

	@Test
	public void pathOverride() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(this.context, this.property + ".path:/mypath");
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().getPath(), equalTo("/mypath"));
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

	@SuppressWarnings("unchecked")
	protected T getEndpointBean() {
		return (T) this.context.getBean(this.type);
	}

}
