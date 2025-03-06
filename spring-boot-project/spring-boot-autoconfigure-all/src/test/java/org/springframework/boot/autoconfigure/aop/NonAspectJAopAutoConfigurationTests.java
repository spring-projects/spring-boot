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

import org.junit.jupiter.api.Test;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AopAutoConfiguration} without AspectJ.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("aspectjweaver*.jar")
class NonAspectJAopAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class));

	@Test
	void whenAspectJIsAbsentAndProxyTargetClassIsEnabledProxyCreatorBeanIsDefined() {
		this.contextRunner.run((context) -> {
			BeanDefinition autoProxyCreator = context.getBeanFactory()
				.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
			assertThat(autoProxyCreator.getPropertyValues().get("proxyTargetClass")).isEqualTo(Boolean.TRUE);
		});
	}

	@Test
	void whenAspectJIsAbsentAndProxyTargetClassIsDisabledNoProxyCreatorBeanIsDefined() {
		this.contextRunner.withPropertyValues("spring.aop.proxy-target-class:false")
			.run((context) -> assertThat(context).doesNotHaveBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME));
	}

}
