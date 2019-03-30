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

package org.springframework.boot.test.autoconfigure.cache;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.ExampleEntity;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code ImportsContextCustomizerFactory} when used with
 * {@link ImportAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ImportsContextCustomizerFactoryWithAutoConfigurationTests {

	static ApplicationContext contextFromTest;

	@Test
	public void testClassesThatHaveSameAnnotationsShareAContext()
			throws InitializationError {
		RunNotifier notifier = new RunNotifier();
		new SpringJUnit4ClassRunner(DataJpaTest1.class).run(notifier);
		ApplicationContext test1Context = contextFromTest;
		new SpringJUnit4ClassRunner(DataJpaTest3.class).run(notifier);
		ApplicationContext test2Context = contextFromTest;
		assertThat(test1Context).isSameAs(test2Context);
	}

	@Test
	public void testClassesThatOnlyHaveDifferingUnrelatedAnnotationsShareAContext()
			throws InitializationError {
		RunNotifier notifier = new RunNotifier();
		new SpringJUnit4ClassRunner(DataJpaTest1.class).run(notifier);
		ApplicationContext test1Context = contextFromTest;
		new SpringJUnit4ClassRunner(DataJpaTest2.class).run(notifier);
		ApplicationContext test2Context = contextFromTest;
		assertThat(test1Context).isSameAs(test2Context);
	}

	@Test
	public void testClassesThatOnlyHaveDifferingPropertyMappedAnnotationAttributesDoNotShareAContext()
			throws InitializationError {
		RunNotifier notifier = new RunNotifier();
		new SpringJUnit4ClassRunner(DataJpaTest1.class).run(notifier);
		ApplicationContext test1Context = contextFromTest;
		new SpringJUnit4ClassRunner(DataJpaTest4.class).run(notifier);
		ApplicationContext test2Context = contextFromTest;
		assertThat(test1Context).isNotSameAs(test2Context);
	}

	@DataJpaTest
	@ContextConfiguration(classes = EmptyConfig.class)
	@Unrelated1
	public static class DataJpaTest1 {

		@Autowired
		private ApplicationContext context;

		@Test
		public void test() {
			contextFromTest = this.context;
		}

	}

	@DataJpaTest
	@ContextConfiguration(classes = EmptyConfig.class)
	@Unrelated2
	public static class DataJpaTest2 {

		@Autowired
		private ApplicationContext context;

		@Test
		public void test() {
			contextFromTest = this.context;
		}

	}

	@DataJpaTest
	@ContextConfiguration(classes = EmptyConfig.class)
	@Unrelated1
	public static class DataJpaTest3 {

		@Autowired
		private ApplicationContext context;

		@Test
		public void test() {
			contextFromTest = this.context;
		}

	}

	@DataJpaTest(showSql = false)
	@ContextConfiguration(classes = EmptyConfig.class)
	@Unrelated1
	public static class DataJpaTest4 {

		@Autowired
		private ApplicationContext context;

		@Test
		public void test() {
			contextFromTest = this.context;
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Unrelated1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Unrelated2 {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = ExampleEntity.class)
	@AutoConfigurationPackage
	static class EmptyConfig {

	}

}
