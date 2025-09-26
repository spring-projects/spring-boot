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

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.internal.GrpcUtil;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration.GrpcServletConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Toshiaki Maki
 */
class GrpcServletAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner() {
		BindableService service = mock();
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		given(service.bindService()).willReturn(serviceDefinition);
		// NOTE: we use noop server lifecycle to avoid startup
		return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class,
				GrpcServerAutoConfiguration.class, GrpcServerFactoryAutoConfiguration.class))
			.withBean(BindableService.class, () -> service);
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServletConfiguration.class)
				.doesNotHaveBean(ServletRegistrationBean.class));
	}

	@Test
	void whenSpringGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServletConfiguration.class));
	}

	@Test
	void whenNoBindableServicesRegisteredAutoConfigurationIsSkipped() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServletConfiguration.class)
				.doesNotHaveBean(ServletRegistrationBean.class));
	}

	@Test
	void whenGrpcServletNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(GrpcServlet.class))
			.withPropertyValues("spring.grpc.server.port=0")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServletConfiguration.class)
				.doesNotHaveBean(ServletRegistrationBean.class));
	}

	@Test
	void whenWebApplicationServletIsAutoConfigured() {
		this.contextRunner().run((context) -> assertThat(context).getBean(ServletRegistrationBean.class).isNotNull());
	}

	@Test
	void whenCustomizerIsRegistered() {
		ServerBuilderCustomizer<ServletServerBuilder> customizer = mock();
		this.contextRunner()
			.withBean(ServerBuilderCustomizer.class, () -> customizer)
			.run((context) -> then(customizer).should().customize(any(ServletServerBuilder.class)));
	}

	@Test
	void whenMaxInboundMessageSizeIsSetThenItIsUsed() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.max-inbound-message-size=10KB")
			.run((context) -> assertThat(context).getBean(ServletRegistrationBean.class)
				.hasFieldOrPropertyWithValue("servlet.servletAdapter.maxInboundMessageSize",
						Math.toIntExact(DataSize.ofKilobytes(10).toBytes())));
	}

	@Test
	void whenMaxInboundMessageSizeIsNotSetThenDefaultIsUsed() {
		this.contextRunner()
			.run((context) -> assertThat(context).getBean(ServletRegistrationBean.class)
				.hasFieldOrPropertyWithValue("servlet.servletAdapter.maxInboundMessageSize",
						GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE));
	}

}
