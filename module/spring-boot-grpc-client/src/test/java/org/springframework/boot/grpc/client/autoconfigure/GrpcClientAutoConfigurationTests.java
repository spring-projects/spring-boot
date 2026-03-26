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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.Codec;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.kotlin.AbstractCoroutineStub;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration.GrpcClientCoroutineStubConfiguration;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.boot.grpc.client.autoconfigure.test.scan.DummyBlockingGrpc;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.InProcessGrpcChannelFactory;
import org.springframework.grpc.client.NettyGrpcChannelFactory;
import org.springframework.grpc.client.ShadedNettyGrpcChannelFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link GrpcClientAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class GrpcClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcClientAutoConfiguration.class, SslAutoConfiguration.class));

	private final ApplicationContextRunner contextRunnerWithoutInProcessChannelFactory = this.contextRunner
		.withPropertyValues("spring.grpc.client.inprocess.enabled=false");

	@Test
	void whenGrpcStubNotOnClasspathThenAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(AbstractStub.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientAutoConfiguration.class));
	}

	@Test
	void whenGrpcKotlinIsNotOnClasspathThenAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(AbstractCoroutineStub.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientCoroutineStubConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.client.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(GrpcClientAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.client.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientAutoConfiguration.class));
	}

	@Test
	void whenHasUserDefinedCredentialsProviderDoesNotAutoConfigureBean() {
		ChannelCredentialsProvider customCredentialsProvider = mock(ChannelCredentialsProvider.class);
		this.contextRunner
			.withBean("customCredentialsProvider", ChannelCredentialsProvider.class, () -> customCredentialsProvider)
			.run((context) -> assertThat(context).getBean(ChannelCredentialsProvider.class)
				.isSameAs(customCredentialsProvider));
	}

	@Test
	void credentialsProviderAutoConfiguredAsExpected() {
		this.contextRunner.run((context) -> assertThat(context).getBean(PropertiesChannelCredentialsProvider.class)
			.hasFieldOrPropertyWithValue("properties", context.getBean(GrpcClientProperties.class))
			.extracting("bundles")
			.isInstanceOf(SslBundles.class));
	}

	@Test
	void clientPropertiesAutoConfiguredResolvesPlaceholders() {
		this.contextRunner
			.withPropertyValues("spring.grpc.client.channel.c1.target=my-server-${channelName}:8888", "channelName=foo")
			.run((context) -> assertThat(context).getBean(GrpcClientProperties.class).satisfies((properties) -> {
				Channel channel = properties.getChannel().get("c1");
				assertThat(channel).isNotNull();
				assertThat(channel.getTarget()).isEqualTo("my-server-foo:8888");
			}));
	}

	@Test
	void clientPropertiesChannelCustomizerAutoConfiguredWithHealthAsExpected() {
		this.contextRunner
			.withPropertyValues("spring.grpc.client.channel.test.health.enabled=true",
					"spring.grpc.client.channel.test.health.service-name=my-service")
			.run((context) -> {
				GrpcChannelBuilderCustomizers customizers = context.getBean(GrpcChannelBuilderCustomizers.class);
				ManagedChannelBuilder<?> builder = Mockito.mock();
				customizers.apply("test", builder);
				Map<String, ?> healthCheckConfig = Map.of("healthCheckConfig", Map.of("serviceName", "my-service"));
				then(builder).should().defaultServiceConfig(healthCheckConfig);
			});
	}

	@Test
	void clientPropertiesChannelCustomizerAutoConfiguredWithoutHealthAsExpected() {
		this.contextRunner.run((context) -> {
			GrpcChannelBuilderCustomizers customizers = context.getBean(GrpcChannelBuilderCustomizers.class);
			ManagedChannelBuilder<?> builder = Mockito.mock();
			customizers.apply("test", builder);
			then(builder).should(never()).defaultServiceConfig(anyMap());
		});
	}

	@Test
	void compressionCustomizerAutoConfiguredAsExpected() {
		this.contextRunner.run((context) -> {
			GrpcChannelBuilderCustomizers customizers = context.getBean(GrpcChannelBuilderCustomizers.class);
			CompressorRegistry compressorRegistry = context.getBean(CompressorRegistry.class);
			ManagedChannelBuilder<?> builder = Mockito.mock();
			customizers.apply("testChannel", builder);
			then(builder).should().compressorRegistry(compressorRegistry);
		});
	}

	@Test
	void compressionCustomizerWhenNoRegistry() {
		// Codec class guards the imported GrpcCodecConfiguration to hide registry
		this.contextRunner.withClassLoader(new FilteredClassLoader(Codec.class)).run((context) -> {
			GrpcChannelBuilderCustomizers customizers = context.getBean(GrpcChannelBuilderCustomizers.class);
			ManagedChannelBuilder<?> builder = Mockito.mock();
			customizers.apply("testChannel", builder);
			then(builder).should(never()).compressorRegistry(any());
		});
	}

	@Test
	void decompressionCustomizerAutoConfiguredAsExpected() {
		this.contextRunner.run((context) -> {
			GrpcChannelBuilderCustomizers customizers = context.getBean(GrpcChannelBuilderCustomizers.class);
			DecompressorRegistry decompressorRegistry = context.getBean(DecompressorRegistry.class);
			ManagedChannelBuilder<?> builder = Mockito.mock();
			customizers.apply("testChannel", builder);
			then(builder).should().decompressorRegistry(decompressorRegistry);
		});
	}

	@Test
	void whenNoDecompressorRegistryThenDecompressionCustomizerIsNotConfigured() {
		// Codec class guards the imported GrpcCodecConfiguration to hide registry
		this.contextRunner.withClassLoader(new FilteredClassLoader(Codec.class)).run((context) -> {
			GrpcChannelBuilderCustomizers customizers = context.getBean(GrpcChannelBuilderCustomizers.class);
			ManagedChannelBuilder<?> builder = Mockito.mock();
			customizers.apply("testChannel", builder);
			then(builder).should(never()).compressorRegistry(any());
		});
	}

	@Test
	void whenInProcessEnabledPropNotSetDoesAutoconfigureInProcess() {
		this.contextRunner.run((context) -> assertThat(context).getBeans(GrpcChannelFactory.class)
			.containsKey("inProcessGrpcChannelFactory"));
	}

	@Test
	void whenInProcessEnabledPropSetToTrueDoesAutoconfigureInProcess() {
		this.contextRunner.withPropertyValues("spring.grpc.client.inprocess.enabled=true")
			.run((context) -> assertThat(context).getBeans(GrpcChannelFactory.class)
				.containsKey("inProcessGrpcChannelFactory"));
	}

	@Test
	void whenInProcessEnabledPropSetToFalseDoesNotAutoconfigureInProcess() {
		this.contextRunner.withPropertyValues("spring.grpc.client.inprocess.enabled=false")
			.run((context) -> assertThat(context).getBeans(GrpcChannelFactory.class)
				.doesNotContainKey("inProcessGrpcChannelFactory"));
	}

	@Test
	void whenInProcessIsNotOnClasspathDoesNotAutoconfigureInProcess() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(InProcessChannelBuilder.class))
			.run((context) -> assertThat(context).getBeans(GrpcChannelFactory.class)
				.doesNotContainKey("inProcessGrpcChannelFactory"));
	}

	@Test
	void whenHasUserDefinedInProcessChannelFactoryDoesNotAutoConfigureBean() {
		InProcessGrpcChannelFactory customChannelFactory = mock();
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(NettyChannelBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.withBean("customChannelFactory", InProcessGrpcChannelFactory.class, () -> customChannelFactory)
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class).isSameAs(customChannelFactory));
	}

	@Test
	void whenHasUserDefinedChannelFactoryDoesNotAutoConfigureNettyOrShadedNetty() {
		GrpcChannelFactory customChannelFactory = mock();
		this.contextRunnerWithoutInProcessChannelFactory
			.withBean("customChannelFactory", GrpcChannelFactory.class, () -> customChannelFactory)
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class).isSameAs(customChannelFactory));
	}

	@Test
	void userDefinedChannelFactoryWithInProcessChannelFactory() {
		GrpcChannelFactory customChannelFactory = mock();
		this.contextRunner.withBean("customChannelFactory", GrpcChannelFactory.class, () -> customChannelFactory)
			.run((context) -> assertThat(context).getBeans(GrpcChannelFactory.class)
				.containsOnlyKeys("customChannelFactory", "inProcessGrpcChannelFactory"));
	}

	@Test
	void whenShadedAndNonShadedNettyOnClasspathShadedNettyFactoryIsAutoConfigured() {
		this.contextRunnerWithoutInProcessChannelFactory
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(ShadedNettyGrpcChannelFactory.class));
	}

	@Test
	void shadedNettyWithInProcessChannelFactory() {
		this.contextRunner.run((context) -> assertThat(context).getBeans(GrpcChannelFactory.class)
			.containsOnlyKeys("shadedNettyGrpcChannelFactory", "inProcessGrpcChannelFactory"));
	}

	@Test
	void whenOnlyNonShadedNettyOnClasspathNonShadedNettyFactoryIsAutoConfigured() {
		this.contextRunnerWithoutInProcessChannelFactory
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(NettyGrpcChannelFactory.class));
	}

	@Test
	void nonShadedNettyWithInProcessChannelFactory() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.run((context) -> assertThat(context).getBeans(GrpcChannelFactory.class)
				.containsOnlyKeys("nettyGrpcChannelFactory", "inProcessGrpcChannelFactory"));
	}

	@Test
	void whenShadedNettyAndNettyNotOnClasspathNoChannelFactoryIsAutoConfigured() {
		this.contextRunnerWithoutInProcessChannelFactory
			.withClassLoader(new FilteredClassLoader(NettyChannelBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcChannelFactory.class));
	}

	@Test
	void noChannelFactoryWithInProcessChannelFactory() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(NettyChannelBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(InProcessGrpcChannelFactory.class));
	}

	@Test
	void shadedNettyChannelFactoryAutoConfiguredAsExpected() {
		this.contextRunnerWithoutInProcessChannelFactory
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(ShadedNettyGrpcChannelFactory.class)
				.hasFieldOrPropertyWithValue("credentials", context.getBean(PropertiesChannelCredentialsProvider.class))
				.extracting("targets")
				.isInstanceOf(PropertiesVirtualTargets.class));
	}

	@Test
	void nettyChannelFactoryAutoConfiguredAsExpected() {
		this.contextRunnerWithoutInProcessChannelFactory
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(NettyGrpcChannelFactory.class)
				.hasFieldOrPropertyWithValue("credentials", context.getBean(PropertiesChannelCredentialsProvider.class))
				.extracting("targets")
				.isInstanceOf(PropertiesVirtualTargets.class));
	}

	@Test
	void inProcessChannelFactoryAutoConfiguredAsExpected() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(NettyChannelBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(InProcessGrpcChannelFactory.class)
				.extracting("credentials")
				.isSameAs(ChannelCredentialsProvider.INSECURE));
	}

	@Test
	void shadedNettyChannelFactoryAutoConfiguredWithCustomizers() {
		io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder builder = mock();
		channelFactoryAutoConfiguredWithCustomizers(this.contextRunnerWithoutInProcessChannelFactory, builder,
				ShadedNettyGrpcChannelFactory.class);
	}

	@Test
	void nettyChannelFactoryAutoConfiguredWithCustomizers() {
		NettyChannelBuilder builder = mock();
		channelFactoryAutoConfiguredWithCustomizers(
				this.contextRunnerWithoutInProcessChannelFactory.withClassLoader(
						new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class)),
				builder, NettyGrpcChannelFactory.class);
	}

	@Test
	void inProcessChannelFactoryAutoConfiguredWithCustomizers() {
		InProcessChannelBuilder builder = mock();
		channelFactoryAutoConfiguredWithCustomizers(
				this.contextRunner.withClassLoader(new FilteredClassLoader(NettyChannelBuilder.class,
						io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class)),
				builder, InProcessGrpcChannelFactory.class);
	}

	private <T extends ManagedChannelBuilder<T>> void channelFactoryAutoConfiguredWithCustomizers(
			ApplicationContextRunner contextRunner, ManagedChannelBuilder<T> mockChannelBuilder,
			Class<?> expectedChannelFactoryType) {
		contextRunner.withUserConfiguration(ChannelBuilderCustomizersConfig.class)
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(expectedChannelFactoryType)
				.extracting("globalCustomizers", InstanceOfAssertFactories.list(GrpcChannelBuilderCustomizer.class))
				.satisfies((allCustomizers) -> {
					allCustomizers.forEach((c) -> c.customize("channel1", mockChannelBuilder));
					InOrder ordered = inOrder(mockChannelBuilder);
					ordered.verify(mockChannelBuilder).keepAliveTime(40L, TimeUnit.SECONDS);
					ordered.verify(mockChannelBuilder).keepAliveTime(50L, TimeUnit.SECONDS);
				}));
	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigurationPackage(basePackageClasses = DummyBlockingGrpc.class)
	static class AutoConfigurePackagesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ChannelBuilderCustomizersConfig {

		@Bean
		@Order(100)
		<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> customizerOne() {
			return (target, builder) -> builder.keepAliveTime(40L, TimeUnit.SECONDS);
		}

		@Bean
		@Order(200)
		<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> customizerTwo() {
			return (target, builder) -> builder.keepAliveTime(50L, TimeUnit.SECONDS);
		}

	}

}
