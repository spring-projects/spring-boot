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

package org.springframework.boot.grpc.server.autoconfigure;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.BindableService;
import io.grpc.Codec;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.Grpc;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.netty.NettyServerBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.ServerServiceDefinitionFilter;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.DefaultGrpcServiceConfigurer;
import org.springframework.grpc.server.service.DefaultGrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.grpc.server.service.ServerInterceptorFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Andrey Litvitski
 */
@SuppressWarnings("rawtypes")
class GrpcServerAutoConfigurationTests {

	private final BindableService service = mock();

	private final ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();

	@BeforeEach
	void prepareForTest() {
		given(this.service.bindService()).willReturn(this.serviceDefinition);
	}

	private ApplicationContextRunner contextRunner() {
		// NOTE: we use noop server lifecycle to avoid startup
		ApplicationContextRunner runner = new ApplicationContextRunner();
		return contextRunner(runner);
	}

	private ApplicationContextRunner contextRunner(ApplicationContextRunner runner) {
		return runner
			.withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class,
					GrpcServerFactoryAutoConfiguration.class, SslAutoConfiguration.class))
			.withBean("shadedNettyGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean("nettyGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean("inProcessGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean(BindableService.class, () -> this.service);
	}

	private WebApplicationContextRunner webContextRunner(WebApplicationContextRunner runner) {
		return runner
			.withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class,
					GrpcServerFactoryAutoConfiguration.class, SslAutoConfiguration.class))
			.withBean("shadedNettyGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean("nettyGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean("inProcessGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean(BindableService.class, () -> this.service);
	}

	private ApplicationContextRunner contextRunnerWithLifecyle() {
		// NOTE: we use noop server lifecycle to avoid startup
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class,
					GrpcServerFactoryAutoConfiguration.class, SslAutoConfiguration.class))
			.withBean(BindableService.class, () -> this.service);
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenSpringGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenNoBindableServicesRegisteredAutoConfigurationIsSkipped() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner().run((context) -> assertThat(context).hasSingleBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenHasUserDefinedGrpcServiceDiscovererDoesNotAutoConfigureBean() {
		GrpcServiceDiscoverer customGrpcServiceDiscoverer = mock(GrpcServiceDiscoverer.class);
		this.contextRunnerWithLifecyle()
			.withBean("customGrpcServiceDiscoverer", GrpcServiceDiscoverer.class, () -> customGrpcServiceDiscoverer)
			.withPropertyValues("spring.grpc.server.port=0")
			.run((context) -> assertThat(context).getBean(GrpcServiceDiscoverer.class)
				.isSameAs(customGrpcServiceDiscoverer));
	}

	@Test
	void grpcServiceDiscovererAutoConfiguredAsExpected() {
		this.contextRunnerWithLifecyle()
			.withPropertyValues("spring.grpc.server.port=0")
			.run((context) -> assertThat(context).getBean(GrpcServiceDiscoverer.class)
				.isInstanceOf(DefaultGrpcServiceDiscoverer.class));
	}

	@Test
	void whenHasUserDefinedServerBuilderCustomizersDoesNotAutoConfigureBean() {
		ServerBuilderCustomizers customCustomizers = mock(ServerBuilderCustomizers.class);
		this.contextRunner()
			.withBean("customCustomizers", ServerBuilderCustomizers.class, () -> customCustomizers)
			.run((context) -> assertThat(context).getBean(ServerBuilderCustomizers.class).isSameAs(customCustomizers));
	}

	@Test
	void serverBuilderCustomizersAutoConfiguredAsExpected() {
		this.contextRunner()
			.withUserConfiguration(ServerBuilderCustomizersConfig.class)
			.run((context) -> assertThat(context).getBean(ServerBuilderCustomizers.class)
				.extracting("customizers", InstanceOfAssertFactories.list(ServerBuilderCustomizer.class))
				.contains(ServerBuilderCustomizersConfig.CUSTOMIZER_BAR,
						ServerBuilderCustomizersConfig.CUSTOMIZER_FOO));
	}

	@Test
	void whenHasUserDefinedServerFactoryDoesNotAutoConfigureBean() {
		GrpcServerFactory customServerFactory = mock(GrpcServerFactory.class);
		this.contextRunner()
			.withBean("customServerFactory", GrpcServerFactory.class, () -> customServerFactory)
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class).isSameAs(customServerFactory));
	}

	@Test
	void userDefinedServerFactoryWithInProcessServerFactory() {
		GrpcServerFactory customServerFactory = mock(GrpcServerFactory.class);
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.withBean("customServerFactory", GrpcServerFactory.class, () -> customServerFactory)
			.run((context) -> assertThat(context).getBeans(GrpcServerFactory.class)
				.containsOnlyKeys("customServerFactory", "inProcessGrpcServerFactory"));
	}

	@Test
	void whenShadedAndNonShadedNettyOnClasspathShadedNettyFactoryIsAutoConfigured() {
		this.contextRunner()
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(ShadedNettyGrpcServerFactory.class));
	}

	@Test
	void shadedNettyFactoryWithInProcessServerFactory() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.run((context) -> assertThat(context).getBeans(GrpcServerFactory.class)
				.containsOnlyKeys("shadedNettyGrpcServerFactory", "inProcessGrpcServerFactory"));
	}

	@Test
	void whenOnlyNonShadedNettyOnClasspathNonShadedNettyFactoryIsAutoConfigured() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(NettyGrpcServerFactory.class));
	}

	@Test
	void nonShadedNettyFactoryWithInProcessServerFactory() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.run((context) -> assertThat(context).getBeans(GrpcServerFactory.class)
				.containsOnlyKeys("nettyGrpcServerFactory", "inProcessGrpcServerFactory"));
	}

	@Test
	void whenShadedNettyAndNettyNotOnClasspathNoServerFactoryIsAutoConfigured() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerFactory.class));
	}

	@Test
	void noServerFactoryWithInProcessServerFactory() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(InProcessGrpcServerFactory.class));
	}

	@Test
	void shadedNettyServerFactoryAutoConfiguredWithCustomLifecycle() {
		GrpcServerLifecycle customServerLifecycle = mock(GrpcServerLifecycle.class);
		this.contextRunnerWithLifecyle()
			.withBean("shadedNettyGrpcServerLifecycle", GrpcServerLifecycle.class, () -> customServerLifecycle)
			.run((context) -> {
				assertThat(context).getBean(GrpcServerFactory.class).isInstanceOf(ShadedNettyGrpcServerFactory.class);
				assertThat(context).getBean("shadedNettyGrpcServerLifecycle", GrpcServerLifecycle.class)
					.isSameAs(customServerLifecycle);
			});
	}

	@Test
	void nettyServerFactoryAutoConfiguredWithCustomLifecycle() {
		GrpcServerLifecycle customServerLifecycle = mock(GrpcServerLifecycle.class);
		this.contextRunnerWithLifecyle()
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.withBean("nettyGrpcServerLifecycle", GrpcServerLifecycle.class, () -> customServerLifecycle)
			.run((context) -> {
				assertThat(context).getBean(GrpcServerFactory.class).isInstanceOf(NettyGrpcServerFactory.class);
				assertThat(context).getBean("nettyGrpcServerLifecycle", GrpcServerLifecycle.class)
					.isSameAs(customServerLifecycle);
			});
	}

	@Test
	void inProcessServerFactoryAutoConfiguredWithCustomLifecycle() {
		GrpcServerLifecycle customServerLifecycle = mock(GrpcServerLifecycle.class);
		this.contextRunnerWithLifecyle()
			.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.withBean("inProcessGrpcServerLifecycle", GrpcServerLifecycle.class, () -> customServerLifecycle)
			.run((context) -> {
				assertThat(context).getBean(GrpcServerFactory.class).isInstanceOf(InProcessGrpcServerFactory.class);
				assertThat(context).getBean("inProcessGrpcServerLifecycle", GrpcServerLifecycle.class)
					.isSameAs(customServerLifecycle);
			});
	}

	@Test
	void shadedNettyServerFactoryAutoConfiguredAsExpected() {
		serverFactoryAutoConfiguredAsExpected(
				this.contextRunner()
					.withPropertyValues("spring.grpc.server.host=myhost", "spring.grpc.server.port=6160"),
				ShadedNettyGrpcServerFactory.class, "myhost:6160", "shadedNettyGrpcServerLifecycle");
	}

	@Test
	void serverFactoryAutoConfiguredInWebAppWhenServletDisabled() {
		serverFactoryAutoConfiguredAsExpected(
				this.webContextRunner(new WebApplicationContextRunner())
					.withPropertyValues("spring.grpc.server.host=myhost", "spring.grpc.server.port=6160")
					.withPropertyValues("spring.grpc.server.servlet.enabled=false"),
				GrpcServerFactory.class, "myhost:6160", "shadedNettyGrpcServerLifecycle");
	}

	@Test
	void nettyServerFactoryAutoConfiguredAsExpected() {
		serverFactoryAutoConfiguredAsExpected(this.contextRunner()
			.withPropertyValues("spring.grpc.server.host=myhost", "spring.grpc.server.port=6160")
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)),
				NettyGrpcServerFactory.class, "myhost:6160", "nettyGrpcServerLifecycle");
	}

	@Test
	void inProcessServerFactoryAutoConfiguredAsExpected() {
		serverFactoryAutoConfiguredAsExpected(
				this.contextRunner()
					.withPropertyValues("spring.grpc.server.inprocess.name=foo")
					.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
							io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)),
				InProcessGrpcServerFactory.class, "foo", "inProcessGrpcServerLifecycle");
	}

	private void serverFactoryAutoConfiguredAsExpected(AbstractApplicationContextRunner<?, ?, ?> contextRunner,
			Class<?> expectedServerFactoryType, String expectedAddress, String expectedLifecycleBeanName) {
		contextRunner.run((context) -> {
			assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(expectedServerFactoryType)
				.hasFieldOrPropertyWithValue("address", expectedAddress)
				.extracting("serviceList", InstanceOfAssertFactories.list(ServerServiceDefinition.class))
				.singleElement()
				.extracting(ServerServiceDefinition::getServiceDescriptor)
				.extracting(ServiceDescriptor::getName)
				.isEqualTo("my-service");
			assertThat(context).getBean(expectedLifecycleBeanName, GrpcServerLifecycle.class).isNotNull();
		});
	}

	@Test
	void shadedNettyServerFactoryAutoConfiguredWithCustomizers() {
		io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder builder = mock();
		serverFactoryAutoConfiguredWithCustomizers(this.contextRunnerWithLifecyle(), builder,
				ShadedNettyGrpcServerFactory.class);
	}

	@Test
	void nettyServerFactoryAutoConfiguredWithCustomizers() {
		// FilteredClassLoader hides the class from the auto-configuration but not from
		// the Java SPI used by ServerBuilder.forPort(int) which by default returns
		// shaded Netty. This results in class cast exception when
		// NettyGrpcServerFactory is expecting a non-shaded server builder. We static
		// mock the builder to return non-shaded Netty - which would happen in
		// real world.
		try (MockedStatic<Grpc> serverBuilderForPort = Mockito.mockStatic(Grpc.class)) {
			serverBuilderForPort.when(() -> Grpc.newServerBuilderForPort(anyInt(), any()))
				.thenAnswer((Answer<NettyServerBuilder>) (invocation) -> NettyServerBuilder
					.forPort(invocation.getArgument(0)));
			NettyServerBuilder builder = mock();
			serverFactoryAutoConfiguredWithCustomizers(this.contextRunnerWithLifecyle()
				.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)),
					builder, NettyGrpcServerFactory.class);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends ServerBuilder<T>> void serverFactoryAutoConfiguredWithCustomizers(
			ApplicationContextRunner contextRunner, ServerBuilder<T> mockServerBuilder,
			Class<?> expectedServerFactoryType) {
		ServerBuilderCustomizer<T> customizer1 = (serverBuilder) -> serverBuilder.keepAliveTime(40L, TimeUnit.SECONDS);
		ServerBuilderCustomizer<T> customizer2 = (serverBuilder) -> serverBuilder.keepAliveTime(50L, TimeUnit.SECONDS);
		ServerBuilderCustomizers customizers = new ServerBuilderCustomizers(List.of(customizer1, customizer2));
		contextRunner.withPropertyValues("spring.grpc.server.port=0", "spring.grpc.server.keep-alive.time=30s")
			.withBean("serverBuilderCustomizers", ServerBuilderCustomizers.class, () -> customizers)
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(expectedServerFactoryType)
				.extracting("serverBuilderCustomizers", InstanceOfAssertFactories.list(ServerBuilderCustomizer.class))
				.satisfies((allCustomizers) -> {
					allCustomizers.forEach((c) -> c.customize(mockServerBuilder));
					InOrder ordered = inOrder(mockServerBuilder);
					ordered.verify(mockServerBuilder)
						.keepAliveTime(Duration.ofSeconds(30L).toNanos(), TimeUnit.NANOSECONDS);
					ordered.verify(mockServerBuilder).keepAliveTime(40L, TimeUnit.SECONDS);
					ordered.verify(mockServerBuilder).keepAliveTime(50L, TimeUnit.SECONDS);
				}));
	}

	@Test
	void nettyServerFactoryAutoConfiguredWithSsl() {
		serverFactoryAutoConfiguredAsExpected(this.contextRunner()
			.withPropertyValues("spring.grpc.server.ssl.bundle=ssltest",
					"spring.ssl.bundle.jks.ssltest.keystore.location=classpath:org/springframework/boot/grpc/server/autoconfigure/test.jks",
					"spring.ssl.bundle.jks.ssltest.keystore.password=secret",
					"spring.ssl.bundle.jks.ssltest.key.password=password", "spring.grpc.server.host=myhost",
					"spring.grpc.server.port=6160")
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)),
				NettyGrpcServerFactory.class, "myhost:6160", "nettyGrpcServerLifecycle");
	}

	@Nested
	class WithCodecConfiguration {

		@SuppressWarnings("unchecked")
		@Test
		void compressionCustomizerAutoConfiguredAsExpected() {
			GrpcServerAutoConfigurationTests.this.contextRunner().run((context) -> {
				assertThat(context).getBean("compressionServerConfigurer", ServerBuilderCustomizer.class).isNotNull();
				var customizer = context.getBean("compressionServerConfigurer", ServerBuilderCustomizer.class);
				var compressorRegistry = context.getBean(CompressorRegistry.class);
				ServerBuilder<?> builder = mock();
				customizer.customize(builder);
				then(builder).should().compressorRegistry(compressorRegistry);
			});
		}

		@Test
		void whenNoCompressorRegistryThenCompressionCustomizerIsNotConfigured() {
			// Codec class guards the imported GrpcCodecConfiguration which provides the
			// registry
			GrpcServerAutoConfigurationTests.this.contextRunner()
				.withClassLoader(new FilteredClassLoader(Codec.class))
				.run((context) -> assertThat(context)
					.getBean("compressionServerConfigurer", ServerBuilderCustomizer.class)
					.isNull());
		}

		@SuppressWarnings("unchecked")
		@Test
		void decompressionCustomizerAutoConfiguredAsExpected() {
			GrpcServerAutoConfigurationTests.this.contextRunner().run((context) -> {
				assertThat(context).getBean("decompressionServerConfigurer", ServerBuilderCustomizer.class).isNotNull();
				var customizer = context.getBean("decompressionServerConfigurer", ServerBuilderCustomizer.class);
				var decompressorRegistry = context.getBean(DecompressorRegistry.class);
				ServerBuilder<?> builder = mock();
				customizer.customize(builder);
				then(builder).should().decompressorRegistry(decompressorRegistry);
			});
		}

		@Test
		void whenNoDecompressorRegistryThenDecompressionCustomizerIsNotConfigured() {
			// Codec class guards the imported GrpcCodecConfiguration which provides the
			// registry
			GrpcServerAutoConfigurationTests.this.contextRunner()
				.withClassLoader(new FilteredClassLoader(Codec.class))
				.run((context) -> assertThat(context)
					.getBean("decompressionClientCustomizer", ServerBuilderCustomizer.class)
					.isNull());
		}

	}

	@Nested
	class WithAllFactoriesServiceFilterAutoConfig {

		@Test
		void whenNoServiceFilterThenFactoryUsesNoFilter() {
			GrpcServerAutoConfigurationTests.this.contextRunner()
				.withPropertyValues("spring.grpc.server.inprocess.name=foo")
				.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
						io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
				.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
					.isInstanceOf(InProcessGrpcServerFactory.class)
					.extracting("serviceFilter")
					.isNull());
		}

		@Test
		void whenUniqueServiceFilterThenFactoryUsesFilter() {
			ServerServiceDefinitionFilter serviceFilter = mock();
			GrpcServerAutoConfigurationTests.this.contextRunner()
				.withPropertyValues("spring.grpc.server.inprocess.name=foo")
				.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
						io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
				.withBean(ServerServiceDefinitionFilter.class, () -> serviceFilter)
				.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
					.isInstanceOf(InProcessGrpcServerFactory.class)
					.extracting("serviceFilter")
					.isSameAs(serviceFilter));
		}

		@Test
		void whenMultipleServiceFiltersThenThrowsException() {
			GrpcServerAutoConfigurationTests.this.contextRunnerWithLifecyle()
				.withPropertyValues("spring.grpc.server.inprocess.name=foo")
				.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
						io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
				.withBean("filter1", ServerServiceDefinitionFilter.class, Mockito::mock)
				.withBean("filter2", ServerServiceDefinitionFilter.class, Mockito::mock)
				.run((context) -> assertThat(context).hasFailed()
					.getFailure()
					.hasMessageContaining("expected single matching bean but found 2: filter1,filter2"));
		}

	}

	@Nested
	class WithGrpcServiceConfigurerAutoConfig {

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			GrpcServiceConfigurer customGrpcServiceConfigurer = mock(GrpcServiceConfigurer.class);
			GrpcServerAutoConfigurationTests.this.contextRunner()
				.withBean("customGrpcServiceConfigurer", GrpcServiceConfigurer.class, () -> customGrpcServiceConfigurer)
				.run((context) -> assertThat(context).getBean(GrpcServiceConfigurer.class)
					.isSameAs(customGrpcServiceConfigurer));
		}

		@Test
		void configurerAutoConfiguredAsExpected() {
			GrpcServerAutoConfigurationTests.this.contextRunnerWithLifecyle()
				.withPropertyValues("spring.grpc.server.port=0")
				.run((context) -> assertThat(context).getBean(GrpcServiceConfigurer.class)
					.isInstanceOf(DefaultGrpcServiceConfigurer.class));
		}

		@Test
		void whenNoServerInterceptorFilterThenConfigurerUsesNoFilter() {
			GrpcServerAutoConfigurationTests.this.contextRunnerWithLifecyle()
				.withPropertyValues("spring.grpc.server.inprocess.name=foo")
				.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
						io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
				.run((context) -> assertThat(context).getBean(InProcessGrpcServerFactory.class)
					.extracting("interceptorFilter")
					.isNull());
		}

		@Test
		void whenUniqueServerInterceptorFilterThenConfigurerUsesFilter() {
			ServerInterceptorFilter interceptorFilter = mock();
			GrpcServerAutoConfigurationTests.this.contextRunnerWithLifecyle()
				.withPropertyValues("spring.grpc.server.inprocess.name=foo")
				.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
						io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
				.withBean(ServerInterceptorFilter.class, () -> interceptorFilter)
				.run((context) -> assertThat(context).getBean(InProcessGrpcServerFactory.class)
					.extracting("interceptorFilter")
					.isSameAs(interceptorFilter));
		}

		@Test
		void whenMultipleServerInterceptorFiltersThenThrowsException() {
			GrpcServerAutoConfigurationTests.this.contextRunnerWithLifecyle()
				.withPropertyValues("spring.grpc.server.inprocess.name=foo")
				.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
						io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
				.withBean("filter1", ServerInterceptorFilter.class, Mockito::mock)
				.withBean("filter2", ServerInterceptorFilter.class, Mockito::mock)
				.run((context) -> assertThat(context).hasFailed()
					.getFailure()
					.hasMessageContaining("expected single matching bean but found 2: filter1,filter2"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServerBuilderCustomizersConfig {

		static ServerBuilderCustomizer<?> CUSTOMIZER_FOO = mock();

		static ServerBuilderCustomizer<?> CUSTOMIZER_BAR = mock();

		@Bean
		@Order(200)
		ServerBuilderCustomizer<?> customizerFoo() {
			return CUSTOMIZER_FOO;
		}

		@Bean
		@Order(100)
		ServerBuilderCustomizer<?> customizerBar() {
			return CUSTOMIZER_BAR;
		}

	}

}
