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

package org.springframework.boot.grpc.client.autoconfigure;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.CompositeGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CompositeChannelFactoryAutoConfiguration}.
 *
 * @author Chris Bono
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class CompositeChannelFactoryAutoConfigurationTests {

	private ApplicationContextRunner contextRunnerWithoutChannelFactories() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcClientAutoConfiguration.class, SslAutoConfiguration.class,
					CompositeChannelFactoryAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class,
					NettyChannelBuilder.class, InProcessChannelBuilder.class));
	}

	@Test
	void whenNoChannelFactoriesDoesNotAutoconfigureComposite() {
		this.contextRunnerWithoutChannelFactories()
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcChannelFactory.class));
	}

	@Test
	void whenSingleChannelFactoryDoesNotAutoconfigureComposite() {
		GrpcChannelFactory channelFactory1 = mock();
		this.contextRunnerWithoutChannelFactories()
			.withBean("channelFactory1", GrpcChannelFactory.class, () -> channelFactory1)
			.run((context) -> assertThat(context).hasSingleBean(GrpcChannelFactory.class)
				.getBean(GrpcChannelFactory.class)
				.isNotInstanceOf(CompositeGrpcChannelFactory.class)
				.isSameAs(channelFactory1));
	}

	@Test
	void whenMultipleChannelFactoriesWithPrimaryDoesNotAutoconfigureComposite() {
		GrpcChannelFactory channelFactory1 = mock();
		GrpcChannelFactory channelFactory2 = mock();
		this.contextRunnerWithoutChannelFactories()
			.withBean("channelFactory1", GrpcChannelFactory.class, () -> channelFactory1)
			.withBean("channelFactory2", GrpcChannelFactory.class, () -> channelFactory2, (bd) -> bd.setPrimary(true))
			.run((context) -> {
				assertThat(context).getBeans(GrpcChannelFactory.class)
					.containsOnlyKeys("channelFactory1", "channelFactory2");
				assertThat(context).getBean(GrpcChannelFactory.class)
					.isNotInstanceOf(CompositeGrpcChannelFactory.class)
					.isSameAs(channelFactory2);
			});
	}

	@Test
	void whenMultipleChannelFactoriesDoesAutoconfigureComposite() {
		GrpcChannelFactory channelFactory1 = mock();
		GrpcChannelFactory channelFactory2 = mock();
		this.contextRunnerWithoutChannelFactories()
			.withBean("channelFactory1", GrpcChannelFactory.class, () -> channelFactory1)
			.withBean("channelFactory2", GrpcChannelFactory.class, () -> channelFactory2)
			.run((context) -> {
				assertThat(context).getBeans(GrpcChannelFactory.class)
					.containsOnlyKeys("channelFactory1", "channelFactory2", "compositeChannelFactory");
				assertThat(context).getBean(GrpcChannelFactory.class).isInstanceOf(CompositeGrpcChannelFactory.class);
			});
	}

	@Test
	void compositeAutoconfiguredAsExpected() {
		this.contextRunnerWithoutChannelFactories()
			.withUserConfiguration(MultipleFactoriesTestConfig.class)
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(CompositeGrpcChannelFactory.class)
				.extracting("channelFactories")
				.asInstanceOf(InstanceOfAssertFactories.list(GrpcChannelFactory.class))
				.containsExactly(MultipleFactoriesTestConfig.CHANNEL_FACTORY_BAR,
						MultipleFactoriesTestConfig.CHANNEL_FACTORY_ZAA,
						MultipleFactoriesTestConfig.CHANNEL_FACTORY_FOO));

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleFactoriesTestConfig {

		static GrpcChannelFactory CHANNEL_FACTORY_FOO = mock();
		static GrpcChannelFactory CHANNEL_FACTORY_BAR = mock();
		static GrpcChannelFactory CHANNEL_FACTORY_ZAA = mock();

		@Bean
		@Order(3)
		GrpcChannelFactory channelFactoryFoo() {
			return CHANNEL_FACTORY_FOO;
		}

		@Bean
		@Order(1)
		GrpcChannelFactory channelFactoryBar() {
			return CHANNEL_FACTORY_BAR;
		}

		@Bean
		@Order(2)
		GrpcChannelFactory channelFactoryZaa() {
			return CHANNEL_FACTORY_ZAA;
		}

	}

}
