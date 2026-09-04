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

package org.springframework.boot.docker.compose.service.connection;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.docker.compose.service.connection.DockerComposeServiceConnectionsApplicationListener.DockerComposeConnectionDetailsBeanRegistrationExcludeFilter;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerComposeConnectionDetailsBeanRegistrationExcludeFilter}.
 */
class DockerComposeConnectionDetailsBeanRegistrationExcludeFilterTests {

	private final DockerComposeConnectionDetailsBeanRegistrationExcludeFilter filter = new DockerComposeConnectionDetailsBeanRegistrationExcludeFilter();

	@Test
	void excludesDockerComposeConnectionDetailsBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(Object.class);
		beanDefinition.setAttribute(DockerComposeConnectionDetailsFactory.class.getName(), true);
		context.registerBeanDefinition("connectionDetails", beanDefinition);
		assertThat(this.filter
			.isExcludedFromAotProcessing(RegisteredBean.of(context.getBeanFactory(), "connectionDetails"))).isTrue();
	}

	@Test
	void doesNotExcludeOtherBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("other", new RootBeanDefinition(Object.class));
		assertThat(this.filter.isExcludedFromAotProcessing(RegisteredBean.of(context.getBeanFactory(), "other")))
			.isFalse();
	}

}
