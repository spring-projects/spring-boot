/*
 * Copyright 2012-2023 the original author or authors.
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
import org.aspectj.weaver.Advice;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AopAutoConfiguration}.
 *
 * @author Eberhard Wolff
 * @author Stephane Nicoll
 */
class AopAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class));

	@Test
	void aopDisabled() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.aop.auto:false")
			.run((context) -> {
				TestAspect aspect = context.getBean(TestAspect.class);
				assertThat(aspect.isCalled()).isFalse();
				TestBean bean = context.getBean(TestBean.class);
				bean.foo();
				assertThat(aspect.isCalled()).isFalse();
			});
	}

	@Test
	void aopWithDefaultSettings() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run(proxyTargetClassEnabled());
	}

	@Test
	void aopWithEnabledProxyTargetClass() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.aop.proxy-target-class:true")
			.run(proxyTargetClassEnabled());
	}

	@Test
	void aopWithDisabledProxyTargetClass() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.aop.proxy-target-class:false")
			.run(proxyTargetClassDisabled());
	}

	@Test
	void customConfigurationWithProxyTargetClassDefaultDoesNotDisableProxying() {
		this.contextRunner.withUserConfiguration(CustomTestConfiguration.class).run(proxyTargetClassEnabled());

	}

	@Test
	void whenGlobalMethodSecurityIsEnabledAndAspectJIsNotAvailableThenClassProxyingIsStillUsedByDefault() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Advice.class))
			.withUserConfiguration(ExampleController.class, EnableGlobalMethodSecurityConfiguration.class)
			.run((context) -> assertThat(context).getBean(ExampleController.class).matches(AopUtils::isCglibProxy));
	}

	private ContextConsumer<AssertableApplicationContext> proxyTargetClassEnabled() {
		return (context) -> {
			TestAspect aspect = context.getBean(TestAspect.class);
			assertThat(aspect.isCalled()).isFalse();
			TestBean bean = context.getBean(TestBean.class);
			bean.foo();
			assertThat(aspect.isCalled()).isTrue();
		};
	}

	private ContextConsumer<AssertableApplicationContext> proxyTargetClassDisabled() {
		return (context) -> {
			TestAspect aspect = context.getBean(TestAspect.class);
			assertThat(aspect.isCalled()).isFalse();
			TestInterface bean = context.getBean(TestInterface.class);
			bean.foo();
			assertThat(aspect.isCalled()).isTrue();
			assertThat(context).doesNotHaveBean(TestBean.class);
		};
	}

	@EnableAspectJAutoProxy
	@Configuration(proxyBeanMethods = false)
	@Import(TestConfiguration.class)
	static class CustomTestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		TestAspect aspect() {
			return new TestAspect();
		}

		@Bean
		TestInterface bean() {
			return new TestBean();
		}

	}

	static class TestBean implements TestInterface {

		@Override
		public void foo() {
		}

	}

	@Aspect
	static class TestAspect {

		private boolean called;

		boolean isCalled() {
			return this.called;
		}

		@Before("execution(* foo(..))")
		void before() {
			this.called = true;
		}

	}

	interface TestInterface {

		void foo();

	}

	@EnableMethodSecurity(prePostEnabled = true)
	@Configuration(proxyBeanMethods = false)
	static class EnableGlobalMethodSecurityConfiguration {

	}

	public static class ExampleController implements TestInterface {

		@RequestMapping("/test")
		@PreAuthorize("true")
		String demo() {
			return "test";
		}

		@Override
		public void foo() {

		}

	}

}
