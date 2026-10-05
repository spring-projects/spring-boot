/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} when {@link AopAutoConfiguration} is excluded.
 *
 * @author Usman Jamali
 */
@WebMvcTest(controllers = ExampleInterfaceController.class, excludeAutoConfiguration = AopAutoConfiguration.class)
class WebMvcTestAopExcludedIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ApplicationContext context;

	@Test
	void autoProxyCreatorIsNotRegistered() {
		assertThat(this.context.containsBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)).isFalse();
	}

	@Test
	void controllerIsNotProxied() {
		assertThat(AopUtils.isAopProxy(this.context.getBean(ExampleInterfaceController.class))).isFalse();
	}

	@Test
	void mappingOnImplementationIsDetected() {
		assertThat(this.mvc.get().uri("/interface")).hasStatusOk().hasBodyTextEqualTo("interface");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class AspectConfiguration {

		@Bean
		ExampleAspect exampleAspect() {
			return new ExampleAspect();
		}

	}

	@Aspect
	static class ExampleAspect {

		@Before("execution(* ping(..))")
		void beforePing() {
		}

	}

}
