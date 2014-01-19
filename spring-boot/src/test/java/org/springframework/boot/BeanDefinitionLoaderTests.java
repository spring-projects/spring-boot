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

package org.springframework.boot;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.sampleconfig.MyComponent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link BeanDefinitionLoader}.
 * 
 * @author Phillip Webb
 */
public class BeanDefinitionLoaderTests {

	private StaticApplicationContext registry;

	@Before
	public void setup() {
		this.registry = new StaticApplicationContext();
	}

	@Test
	public void loadClass() throws Exception {
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry,
				MyComponent.class);
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myComponent"));
	}

	@Test
	public void loadXmlResource() throws Exception {
		ClassPathResource resource = new ClassPathResource("sample-beans.xml", getClass());
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry, resource);
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myXmlComponent"));

	}

	@Test
	public void loadGroovyResource() throws Exception {
		ClassPathResource resource = new ClassPathResource("sample-beans.groovy",
				getClass());
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry, resource);
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myGroovyComponent"));

	}

	@Test
	public void loadPackage() throws Exception {
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry,
				MyComponent.class.getPackage());
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myComponent"));
	}

	@Test
	public void loadClassName() throws Exception {
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry,
				MyComponent.class.getName());
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myComponent"));
	}

	@Test
	public void loadResourceName() throws Exception {
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry,
				"classpath:org/springframework/boot/sample-beans.xml");
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myXmlComponent"));
	}

	@Test
	public void loadGroovyName() throws Exception {
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry,
				"classpath:org/springframework/boot/sample-beans.groovy");
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myGroovyComponent"));
	}

	@Test
	public void loadPackageName() throws Exception {
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry,
				MyComponent.class.getPackage().getName());
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myComponent"));
	}

	@Test
	public void loadPackageAndClassDoesNotDoubleAdd() throws Exception {
		BeanDefinitionLoader loader = new BeanDefinitionLoader(this.registry,
				MyComponent.class.getPackage(), MyComponent.class);
		int loaded = loader.load();
		assertThat(loaded, equalTo(1));
		assertTrue(this.registry.containsBean("myComponent"));
	}

}
