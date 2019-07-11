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

package org.springframework.boot.autoconfigure.jms.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jms.connection.CachingConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ActiveMQConnectionFactoryConfiguration} when
 * {@link CachingConnectionFactory} is not on the classpath.
 *
 * @author Dmytro Nosan
 */
public class ActiveMQAutoConfigurationTestsWithoutCachingConnectionFactoryTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ActiveMQAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(CachingConnectionFactory.class));

	@Test
	public void cachingConnectionFactoryNotOnTheClasspathThenSimpleConnectionFactoryAutoConfigured() {
		this.contextRunner.withPropertyValues("spring.activemq.pool.enabled=false", "spring.jms.cache.enabled=false")
				.run((context) -> assertThat(context).hasSingleBean(ActiveMQConnectionFactory.class));
	}

	@Test
	public void cachingConnectionFactoryNotOnTheClasspathAndCacheEnabledThenSimpleConnectionFactoryNotConfigured() {
		this.contextRunner.withPropertyValues("spring.activemq.pool.enabled=false", "spring.jms.cache.enabled=true")
				.run((context) -> assertThat(context).doesNotHaveBean(ActiveMQConnectionFactory.class));
	}

}
