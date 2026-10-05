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

import org.junit.jupiter.api.Test;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} when AOP auto-configuration is active and no
 * aspect applies to the controller.
 *
 * @author Usman Jamali
 */
@WebMvcTest(controllers = ExampleInterfaceController.class)
class WebMvcTestAopAutoProxyCreatorIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ApplicationContext context;

	@Test
	void autoProxyCreatorIsRegistered() {
		assertThat(this.context.containsBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)).isTrue();
		assertThat(this.context.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME))
			.isInstanceOf(AbstractAutoProxyCreator.class);
	}

	@Test
	void controllerIsNotProxiedWhenNoAdviceMatches() {
		assertThat(AopUtils.isAopProxy(this.context.getBean(ExampleInterfaceController.class))).isFalse();
	}

	@Test
	void mappingOnImplementationIsDetected() {
		assertThat(this.mvc.get().uri("/interface")).hasStatusOk().hasBodyTextEqualTo("interface");
	}

}
