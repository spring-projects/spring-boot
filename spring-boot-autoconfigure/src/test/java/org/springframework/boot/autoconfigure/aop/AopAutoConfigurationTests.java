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

package org.springframework.boot.autoconfigure.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link AopAutoConfiguration}.
 *
 * @author Eberhard Wolff
 */
public class AopAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testNoAopAutoConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, AopAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "spring.aop.auto:false");
		this.context.refresh();
		TestAspect aspect = this.context.getBean(TestAspect.class);
		assertFalse(aspect.isCalled());
		TestBean bean = this.context.getBean(TestBean.class);
		bean.foo();
		assertFalse(aspect.isCalled());
	}

	@Test
	public void testAopAutoConfigurationProxyTargetClass() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, AopAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.aop.proxyTargetClass:true");
		this.context.refresh();
		TestAspect aspect = this.context.getBean(TestAspect.class);
		assertFalse(aspect.isCalled());
		TestBean bean = this.context.getBean(TestBean.class);
		bean.foo();
		assertTrue(aspect.isCalled());
	}

	@Test
	public void testAopAutoConfigurationNoProxyTargetClass() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, AopAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.aop.proxyTargetClass:false");
		this.context.refresh();
		TestAspect aspect = this.context.getBean(TestAspect.class);
		assertFalse(aspect.isCalled());
		TestInterface bean = this.context.getBean(TestInterface.class);
		bean.foo();
		assertTrue(aspect.isCalled());
	}

	@Configuration
	protected static class TestConfiguration {
		@Bean
		public TestAspect aspect() {
			return new TestAspect();
		}

		@Bean
		public TestInterface bean() {
			return new TestBean();
		}
	}

	protected static class TestBean implements TestInterface {
		@Override
		public void foo() {
		}
	}

	@Aspect
	protected static class TestAspect {
		private boolean called;

		public boolean isCalled() {
			return this.called;
		}

		@Before("execution(* foo(..))")
		public void before() {
			this.called = true;
		}
	}

	public interface TestInterface {

		void foo();

	}

}
