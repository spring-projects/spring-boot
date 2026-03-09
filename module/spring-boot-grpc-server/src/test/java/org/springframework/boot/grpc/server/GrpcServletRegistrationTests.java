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

package org.springframework.boot.grpc.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration.Dynamic;
import org.junit.jupiter.api.Test;

import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceInfo;
import org.springframework.grpc.server.service.GrpcServiceSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link GrpcServletRegistration}.
 *
 * @author Phillip Webb
 */
class GrpcServletRegistrationTests {

	private final GrpcServiceConfigurer serviceConfigurer = mock();

	private final GrpcServiceDiscoverer serviceDiscoverer = mock();

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenServiceDiscovererIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GrpcServletRegistration(null, this.serviceConfigurer))
			.withMessage("'serviceDiscoverer' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenServiceConfigurerIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GrpcServletRegistration(this.serviceDiscoverer, null))
			.withMessage("'serviceConfigurer' must not be null");
	}

	@Test
	void createWhenServerBuilderCustomizerIsNullDoesNotApplyCustomization() {
		assertThatNoException()
			.isThrownBy(() -> new GrpcServletRegistration(this.serviceDiscoverer, this.serviceConfigurer, null));
	}

	@Test
	void createWhenServerBuilderCustomizerIsNotNullAppliesCustomization() {
		Consumer<ServletServerBuilder> serverBuilderCustomizer = mock();
		new GrpcServletRegistration(this.serviceDiscoverer, this.serviceConfigurer, serverBuilderCustomizer);
		then(serverBuilderCustomizer).should().accept(any(ServletServerBuilder.class));
	}

	@Test
	void addRegistrationAddsBuiltServlet() {
		GrpcServletRegistration registration = new GrpcServletRegistration(this.serviceDiscoverer,
				this.serviceConfigurer);
		ServletContext servletContext = mock();
		Dynamic result = mock();
		given(servletContext.addServlet(eq("grpcServlet"), any(GrpcServlet.class))).willReturn(result);
		assertThat(registration.addRegistration("test", servletContext)).isEqualTo(result);
	}

	@Test
	void onStartupWhenHasServicesRegistersAndAddsUrlMappingsBasedOnDescriptorName() throws ServletException {
		BindableService service1 = mock(BindableService.class);
		ServerServiceDefinition serviceDefinition1 = ServerServiceDefinition.builder("s1").build();
		given(service1.bindService()).willReturn(serviceDefinition1);
		GrpcServiceInfo info1 = new GrpcServiceInfo(emptyServiceInterceptors(), new String[0], false);
		BindableService service2 = mock(BindableService.class);
		ServerServiceDefinition serviceDefinition2 = ServerServiceDefinition.builder("s2").build();
		given(service2.bindService()).willReturn(serviceDefinition2);
		GrpcServiceInfo info2 = new GrpcServiceInfo(emptyServiceInterceptors(), new String[0], false);
		List<GrpcServiceSpec> specs = new ArrayList<>();
		specs.add(new GrpcServiceSpec(service1, info1));
		specs.add(new GrpcServiceSpec(service2, info2));
		given(this.serviceDiscoverer.findServices()).willReturn(specs);
		given(this.serviceConfigurer.configure(any(GrpcServiceSpec.class), eq(null))).willAnswer((invocation) -> {
			GrpcServiceSpec spec = invocation.getArgument(0, GrpcServiceSpec.class);
			return spec.service().bindService();
		});
		GrpcServletRegistration registration = new GrpcServletRegistration(this.serviceDiscoverer,
				this.serviceConfigurer);
		ServletContext servletContext = mock(ServletContext.class);
		Dynamic result = mock(Dynamic.class);
		given(servletContext.addServlet(eq("grpcServlet"), any(GrpcServlet.class))).willReturn(result);
		registration.onStartup(servletContext);
		then(result).should().addMapping("/s1/*", "/s2/*");
	}

	@Test
	void onStartupWhenHasNoServicesDoesNotAddUrlMappings() throws ServletException {
		given(this.serviceDiscoverer.findServices()).willReturn(Collections.emptyList());
		given(this.serviceConfigurer.configure(any(GrpcServiceSpec.class), eq(null))).willAnswer((invocation) -> {
			GrpcServiceSpec spec = invocation.getArgument(0, GrpcServiceSpec.class);
			return spec.service().bindService();
		});
		GrpcServletRegistration registration = new GrpcServletRegistration(this.serviceDiscoverer,
				this.serviceConfigurer);
		ServletContext servletContext = mock(ServletContext.class);
		Dynamic result = mock(Dynamic.class);
		given(servletContext.addServlet(eq("grpcServlet"), any(GrpcServlet.class))).willReturn(result);
		registration.onStartup(servletContext);
		then(result).should(never()).addMapping();
	}

	@Test
	void getDescriptionReturnsDeducedServletName() {
		GrpcServletRegistration registration = new GrpcServletRegistration(this.serviceDiscoverer,
				this.serviceConfigurer);
		assertThat(registration.getDescription()).isEqualTo("grpcServlet");
	}

	@SuppressWarnings("unchecked")
	private Class<? extends ServerInterceptor>[] emptyServiceInterceptors() {
		return (Class<? extends ServerInterceptor>[]) new Class<?>[] {};
	}

}
