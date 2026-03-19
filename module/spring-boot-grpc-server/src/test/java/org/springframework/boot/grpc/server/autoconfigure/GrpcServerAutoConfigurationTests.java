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

import java.util.concurrent.atomic.AtomicReference;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.DefaultGrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests fir {@link GrpcServerAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Andrey Litvitski
 * @author Phillip Webb
 */
class GrpcServerAutoConfigurationTests {

	private static final AutoConfigurations autoConfigurations = AutoConfigurations
		.of(GrpcServerAutoConfiguration.class, SslAutoConfiguration.class);

	private final BindableService service = mock();

	private final ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(autoConfigurations)
		.with(this::noOpLifecycleBeans)
		.with(this::serviceBean);

	@BeforeEach
	void setup() {
		given(this.service.bindService()).willReturn(this.serviceDefinition);
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenSpringGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenNoBindableServicesRegisteredAutoConfigurationIsSkipped() {
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenHasUserDefinedGrpcServiceDiscovererDoesNotAutoConfigureBean() {
		GrpcServiceDiscoverer customGrpcServiceDiscoverer = mock(GrpcServiceDiscoverer.class);
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.with(this::noOpLifecycleBeans)
			.withBean("customGrpcServiceDiscoverer", GrpcServiceDiscoverer.class, () -> customGrpcServiceDiscoverer)
			.withPropertyValues("spring.grpc.server.port=0")
			.run((context) -> assertThat(context).getBean(GrpcServiceDiscoverer.class)
				.isSameAs(customGrpcServiceDiscoverer));
	}

	@Test
	void grpcServiceDiscovererAutoConfiguredAsExpected() {
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.with(this::serviceBean)
			.run((context) -> assertThat(context).getBean(GrpcServiceDiscoverer.class)
				.isInstanceOf(DefaultGrpcServiceDiscoverer.class));
	}

	@Test
	void serverBuilderCustomizersAutoConfiguredAsExpected() {
		this.contextRunner.withUserConfiguration(ServerBuilderCustomizersConfig.class)
			.run((context) -> assertThat(context).getBean(GrpcServerBuilderCustomizers.class)
				.extracting("customizers", InstanceOfAssertFactories.list(ServerBuilderCustomizer.class))
				.contains(ServerBuilderCustomizersConfig.bar, ServerBuilderCustomizersConfig.foo));
	}

	@Test
	void customizersAreAppliedToNettyServer() {
		AtomicReference<NettyServerBuilder> applied = new AtomicReference<>();
		ServerBuilderCustomizer<NettyServerBuilder> customizer = applied::set;
		this.contextRunner.withBean(ServerBuilderCustomizer.class, () -> customizer)
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run((context) -> {
				context.getBean(GrpcServerFactory.class).createServer();
				assertThat(applied.get()).isInstanceOf(NettyServerBuilder.class);
			});
	}

	@Test
	void customizersAreAppliedToShadedNettyServer() {
		AtomicReference<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder> applied = new AtomicReference<>();
		ServerBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder> customizer = applied::set;
		this.contextRunner.withBean(ServerBuilderCustomizer.class, () -> customizer)
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class))
			.run((context) -> {
				context.getBean(GrpcServerFactory.class).createServer();
				assertThat(applied.get()).isInstanceOf(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class);
			});
	}

	@Test
	void customizersAreAppliedToInProcessServer() {
		AtomicReference<InProcessServerBuilder> applied = new AtomicReference<>();
		ServerBuilderCustomizer<InProcessServerBuilder> customizer = applied::set;
		this.contextRunner.withBean(ServerBuilderCustomizer.class, () -> customizer)
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.withPropertyValues("spring.grpc.server.inprocess.name=test")
			.run((context) -> {
				context.getBean(GrpcServerFactory.class).createServer();
				assertThat(applied.get()).isInstanceOf(InProcessServerBuilder.class);
			});
	}

	@Test
	void whenHasUserDefinedServerFactoryDoesNotAutoConfigureBean() {
		GrpcServerFactory customServerFactory = mock(GrpcServerFactory.class);
		this.contextRunner.withBean("customServerFactory", GrpcServerFactory.class, () -> customServerFactory)
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class).isSameAs(customServerFactory));
	}

	@Test
	void userDefinedServerFactoryWithInProcessServerFactory() {
		GrpcServerFactory customServerFactory = mock(GrpcServerFactory.class);
		this.contextRunner.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.withBean("customServerFactory", GrpcServerFactory.class, () -> customServerFactory)
			.run((context) -> assertThat(context).getBeans(GrpcServerFactory.class)
				.containsOnlyKeys("customServerFactory", "inProcessGrpcServerFactory"));
	}

	@Test
	void whenShadedAndNonShadedNettyOnClasspathShadedNettyFactoryIsAutoConfigured() {
		this.contextRunner.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
			.isInstanceOf(ShadedNettyGrpcServerFactory.class));
	}

	@Test
	void shadedNettyFactoryWithInProcessServerFactory() {
		this.contextRunner.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.run((context) -> assertThat(context).getBeans(GrpcServerFactory.class)
				.containsOnlyKeys("shadedNettyGrpcServerFactory", "inProcessGrpcServerFactory"));
	}

	@Test
	void whenOnlyNonShadedNettyOnClasspathNonShadedNettyFactoryIsAutoConfigured() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(NettyGrpcServerFactory.class));
	}

	@Test
	void nonShadedNettyFactoryWithInProcessServerFactory() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.run((context) -> assertThat(context).getBeans(GrpcServerFactory.class)
				.containsOnlyKeys("nettyGrpcServerFactory", "inProcessGrpcServerFactory"));
	}

	@Test
	void whenShadedNettyAndNettyNotOnClasspathNoServerFactoryIsAutoConfigured() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerFactory.class));
	}

	@Test
	void noServerFactoryWithInProcessServerFactory() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(InProcessGrpcServerFactory.class));
	}

	@Test
	void shadedNettyServerFactoryAutoConfiguredWithCustomLifecycle() {
		GrpcServerLifecycle customServerLifecycle = mock(GrpcServerLifecycle.class);
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.with(this::serviceBean)
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
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.with(this::serviceBean)
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
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.with(this::serviceBean)
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
		this.contextRunner.withPropertyValues("spring.grpc.server.address=192.168.0.1", "spring.grpc.server.port=6160")
			.run(assertThatServerIsConfigured(ShadedNettyGrpcServerFactory.class, "192.168.0.1:6160",
					"shadedNettyGrpcServerLifecycle"));
	}

	@Test
	void nettyServerFactoryAutoConfiguredAsExpected() {
		this.contextRunner.withPropertyValues("spring.grpc.server.address=192.168.0.1", "spring.grpc.server.port=6160")
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run(assertThatServerIsConfigured(NettyGrpcServerFactory.class, "192.168.0.1:6160",
					"nettyGrpcServerLifecycle"));
	}

	@Test
	void serverFactoryAutoConfiguredInWebAppWhenServletDisabled() {
		new WebApplicationContextRunner().withConfiguration(autoConfigurations)
			.with(this::noOpLifecycleBeans)
			.with(this::serviceBean)
			.withPropertyValues("spring.grpc.server.address=192.168.0.1")
			.withPropertyValues("spring.grpc.server.port=6160")
			.withPropertyValues("spring.grpc.server.servlet.enabled=false")
			.run(assertThatServerIsConfigured(ShadedNettyGrpcServerFactory.class, "192.168.0.1:6160",
					"shadedNettyGrpcServerLifecycle"));
	}

	@Test
	void inProcessServerFactoryAutoConfiguredAsExpected() {
		this.contextRunner.withPropertyValues("spring.grpc.server.inprocess.name=foo")
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run(assertThatServerIsConfigured(InProcessGrpcServerFactory.class, "foo", "inProcessGrpcServerLifecycle"));
	}

	@Test
	void nettyServerFactoryAutoConfiguredWithSsl() {
		this.contextRunner.withPropertyValues("spring.grpc.server.address=192.168.0.1", "spring.grpc.server.port=6160",
				"spring.grpc.server.ssl.bundle=ssltest",
				"spring.ssl.bundle.jks.ssltest.keystore.location=classpath:org/springframework/boot/grpc/server/autoconfigure/test.jks",
				"spring.ssl.bundle.jks.ssltest.keystore.password=secret",
				"spring.ssl.bundle.jks.ssltest.key.password=password")
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run(assertThatServerIsConfigured(NettyGrpcServerFactory.class, "192.168.0.1:6160",
					"nettyGrpcServerLifecycle"));
	}

	private ContextConsumer<? super ApplicationContextAssertProvider<?>> assertThatServerIsConfigured(
			Class<?> expectedServerFactoryType, String expectedAddress, String expectedLifecycleBeanName) {
		return (context) -> {
			assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(expectedServerFactoryType)
				.hasFieldOrPropertyWithValue("address", expectedAddress)
				.extracting("serviceList", InstanceOfAssertFactories.list(ServerServiceDefinition.class))
				.singleElement()
				.extracting(ServerServiceDefinition::getServiceDescriptor)
				.extracting(ServiceDescriptor::getName)
				.isEqualTo("my-service");
			assertThat(context).getBean(expectedLifecycleBeanName, GrpcServerLifecycle.class).isNotNull();
		};
	}

	private <R extends AbstractApplicationContextRunner<R, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> R serviceBean(
			R contextRunner) {
		return contextRunner.withBean(BindableService.class, () -> this.service);
	}

	private <R extends AbstractApplicationContextRunner<R, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> R noOpLifecycleBeans(
			R contextRunner) {
		return contextRunner.withBean("shadedNettyGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean("nettyGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean("inProcessGrpcServerLifecycle", GrpcServerLifecycle.class, Mockito::mock);
	}

	@Nested
	class ServletServerAutoConfigurationTests {

		private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(autoConfigurations)
			.with(GrpcServerAutoConfigurationTests.this::serviceBean)
			.withPropertyValues("server.http2.enabled=true");

		@Test
		void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
			this.contextRunner.withClassLoader(new FilteredClassLoader(BindableService.class))
				.run((context) -> assertThat(context).doesNotHaveBean(ServletGrpcServerConfiguration.class)
					.doesNotHaveBean(ServletRegistrationBean.class));
		}

		@Test
		void whenSpringGrpcNotOnClasspathAutoConfigurationIsSkipped() {
			this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class))
				.run((context) -> assertThat(context).doesNotHaveBean(ServletGrpcServerConfiguration.class));
		}

		@Test
		void whenNoBindableServicesRegisteredAutoConfigurationIsSkipped() {
			new WebApplicationContextRunner().withConfiguration(autoConfigurations)
				.run((context) -> assertThat(context).doesNotHaveBean(ServletGrpcServerConfiguration.class)
					.doesNotHaveBean(ServletRegistrationBean.class));
		}

		@Test
		void whenGrpcServletNotOnClasspathAutoConfigurationIsSkipped() {
			this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcServlet.class))
				.withPropertyValues("spring.grpc.server.port=0")
				.run((context) -> assertThat(context).doesNotHaveBean(ServletGrpcServerConfiguration.class)
					.doesNotHaveBean(ServletRegistrationBean.class));
		}

		@Test
		void whenWebApplicationServletIsAutoConfigured() {
			this.contextRunner.run((context) -> assertThat(context).hasSingleBean(GrpcServletRegistration.class));
		}

		@Test
		void whenServerBuilderCustomizerIsRegistered() {
			ServerBuilderCustomizer<ServletServerBuilder> customizer = mock();
			this.contextRunner.withBean(ServerBuilderCustomizer.class, () -> customizer)
				.run((context) -> then(customizer).should().customize(any(ServletServerBuilder.class)));
		}

		@Test
		void whenMaxInboundMessageSizeIsSetThenItIsUsed() {
			this.contextRunner.withPropertyValues("spring.grpc.server.inbound.message.max-size=10KB")
				.run((context) -> assertThat(context).getBean(GrpcServletRegistration.class)
					.hasFieldOrPropertyWithValue("servlet.servletAdapter.maxInboundMessageSize",
							Math.toIntExact(DataSize.ofKilobytes(10).toBytes())));
		}

		@Test
		void whenMaxInboundMessageSizeIsNotSetThenDefaultIsUsed() {
			this.contextRunner.run((context) -> assertThat(context).getBean(GrpcServletRegistration.class)
				.hasFieldOrPropertyWithValue("servlet.servletAdapter.maxInboundMessageSize",
						GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE));
		}

		@Test
		void customizersAreAppliedToServletServer() {
			AtomicReference<ServletServerBuilder> applied = new AtomicReference<>();
			ServerBuilderCustomizer<ServletServerBuilder> customizer = applied::set;
			this.contextRunner.withBean(ServerBuilderCustomizer.class, () -> customizer)
				.run((context) -> assertThat(applied.get()).isInstanceOf(ServletServerBuilder.class));
		}

		@Test
		void whenHttp2EnabledPropertyMissing() {
			new WebApplicationContextRunner().withConfiguration(autoConfigurations)
				.with(GrpcServerAutoConfigurationTests.this::serviceBean)
				.run((context) -> assertThat(context).getFailure()
					.hasMessageContaining(
							"Configuration property 'server.http2.enabled' should be set to true for gRPC support"));
		}

		@Test
		void whenHttp2EnabledPropertyFalse() {
			new WebApplicationContextRunner().withConfiguration(autoConfigurations)
				.with(GrpcServerAutoConfigurationTests.this::serviceBean)
				.withPropertyValues("server.http2.enabled=false")
				.run((context) -> assertThat(context).getFailure()
					.hasMessageContaining(
							"Configuration property 'server.http2.enabled' should be set to true for gRPC support"));
		}

		@Test
		void whenHttp2EnabledPropertyMissingAndValidationDisabled() {
			new WebApplicationContextRunner().withConfiguration(autoConfigurations)
				.with(GrpcServerAutoConfigurationTests.this::serviceBean)
				.withPropertyValues("spring.grpc.server.servlet.validate-http2=false")
				.run((context) -> assertThat(context).hasNotFailed());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServerBuilderCustomizersConfig {

		static ServerBuilderCustomizer<?> foo = mock();

		static ServerBuilderCustomizer<?> bar = mock();

		@Bean
		@Order(200)
		ServerBuilderCustomizer<?> customizerFoo() {
			return foo;
		}

		@Bean
		@Order(100)
		ServerBuilderCustomizer<?> customizerBar() {
			return bar;
		}

	}

}
