/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.reactive.context;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.annotation.Lazy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationConfigReactiveWebApplicationContext}
 *
 * @author Stephane Nicoll
 */
public class AnnotationConfigReactiveWebApplicationContextTests {

	private AnnotationConfigReactiveWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void registerBean() {
		this.context = new AnnotationConfigReactiveWebApplicationContext();
		this.context.registerBean(TestBean.class);
		this.context.refresh();
		assertThat(this.context.getBeanFactory().containsSingleton(
				"annotationConfigReactiveWebApplicationContextTests.TestBean")).isTrue();
		assertThat(this.context.getBean(TestBean.class)).isNotNull();
	}

	@Test
	public void registerBeanWithLazy() {
		this.context = new AnnotationConfigReactiveWebApplicationContext();
		this.context.registerBean(TestBean.class, Lazy.class);
		this.context.refresh();
		assertThat(this.context.getBeanFactory().containsSingleton(
				"annotationConfigReactiveWebApplicationContextTests.TestBean")).isFalse();
		assertThat(this.context.getBean(TestBean.class)).isNotNull();
	}

	@Test
	public void registerBeanWithSupplier() {
		this.context = new AnnotationConfigReactiveWebApplicationContext();
		this.context.registerBean(TestBean.class, TestBean::new);
		this.context.refresh();
		assertThat(this.context.getBeanFactory().containsSingleton(
				"annotationConfigReactiveWebApplicationContextTests.TestBean")).isTrue();
		assertThat(this.context.getBean(TestBean.class)).isNotNull();
	}

	@Test
	public void registerBeanWithSupplierAndLazy() {
		this.context = new AnnotationConfigReactiveWebApplicationContext();
		this.context.registerBean(TestBean.class, TestBean::new, Lazy.class);
		this.context.refresh();
		assertThat(this.context.getBeanFactory().containsSingleton(
				"annotationConfigReactiveWebApplicationContextTests.TestBean")).isFalse();
		assertThat(this.context.getBean(TestBean.class)).isNotNull();
	}

	private static class TestBean {

	}

}
