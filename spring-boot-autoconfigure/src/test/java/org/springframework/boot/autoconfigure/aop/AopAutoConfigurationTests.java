/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AopAutoConfiguration}.
 *
 * @author Eberhard Wolff
 * @author Stephane Nicoll
 */
public class AopAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void aopDisabled() {
		load(TestConfiguration.class, "spring.aop.auto:false");
		TestAspect aspect = this.context.getBean(TestAspect.class);
		assertThat(aspect.isCalled()).isFalse();
		TestBean bean = this.context.getBean(TestBean.class);
		bean.foo();
		assertThat(aspect.isCalled()).isFalse();
	}

	@Test
	public void aopWithDefaultSettings() {
		load(TestConfiguration.class);
		testProxyTargetClassDisabled();
	}

	@Test
	public void aopWithEnabledProxyTargetClass() {
		load(TestConfiguration.class, "spring.aop.proxy-target-class:true");
		testProxyTargetClassEnabled();
	}

	@Test
	public void aopWithDisabledProxyTargetClass() {
		load(TestConfiguration.class, "spring.aop.proxy-target-class:false");
		testProxyTargetClassDisabled();
	}

	@Test
	public void aopWithCustomConfiguration() {
		load(CustomTestConfiguration.class);
		testProxyTargetClassEnabled();
	}

	private void testProxyTargetClassEnabled() {
		TestAspect aspect = this.context.getBean(TestAspect.class);
		assertThat(aspect.isCalled()).isFalse();
		TestBean bean = this.context.getBean(TestBean.class);
		bean.foo();
		assertThat(aspect.isCalled()).isTrue();
	}

	private void testProxyTargetClassDisabled() {
		TestAspect aspect = this.context.getBean(TestAspect.class);
		assertThat(aspect.isCalled()).isFalse();
		TestInterface bean = this.context.getBean(TestInterface.class);
		bean.foo();
		assertThat(aspect.isCalled()).isTrue();
		assertThat(this.context.getBeansOfType(TestBean.class)).isEmpty();
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(config);
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.register(AopAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@EnableAspectJAutoProxy(proxyTargetClass = true)
	@Configuration
	@Import(TestConfiguration.class)
	protected static class CustomTestConfiguration {

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
