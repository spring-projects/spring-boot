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
import java.util.List;
import java.util.function.Consumer;

import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration.Dynamic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.servlet.DynamicRegistrationBean;
import org.springframework.core.log.LogMessage;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceSpec;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link DynamicRegistrationBean} that can be used to register a {@link GrpcServlet}.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Toshiaki Maki
 * @author Phillip Webb
 * @since 4.0.0
 */
public class GrpcServletRegistration extends DynamicRegistrationBean<Dynamic> {

	private static final Log logger = LogFactory.getLog(GrpcServletRegistration.class);

	private final GrpcServlet servlet;

	private final String[] urlMappings;

	/**
	 * Create a new {@link GrpcServletRegistration} instance.
	 * @param serviceDiscoverer the gRPC service discoverer
	 * @param serviceConfigurer the gRPC service configurer
	 */
	public GrpcServletRegistration(GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer) {
		this(serviceDiscoverer, serviceConfigurer, null);
	}

	/**
	 * Create a new {@link GrpcServletRegistration} instance.
	 * @param serviceDiscoverer the gRPC service discoverer
	 * @param serviceConfigurer the gRPC service configurer
	 * @param serverBuilderCustomizer an optional customizer to configure the
	 * {@link ServletServerBuilder}
	 */
	public GrpcServletRegistration(GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
			@Nullable Consumer<ServletServerBuilder> serverBuilderCustomizer) {
		Assert.notNull(serviceDiscoverer, "'serviceDiscoverer' must not be null");
		Assert.notNull(serviceConfigurer, "'serviceConfigurer' must not be null");
		ServletServerBuilder builder = new ServletServerBuilder();
		List<String> urlMappings = new ArrayList<>();
		for (GrpcServiceSpec spec : serviceDiscoverer.findServices()) {
			ServiceDescriptor descriptor = spec.service().bindService().getServiceDescriptor();
			logger.info(LogMessage.format("Registering servlet gRPC service: %s", descriptor.getName()));
			urlMappings.add("/" + descriptor.getName() + "/*");
			ServerServiceDefinition definition = serviceConfigurer.configure(spec, null);
			builder.addService(definition);
		}
		if (serverBuilderCustomizer != null) {
			serverBuilderCustomizer.accept(builder);
		}
		this.servlet = builder.buildServlet();
		this.urlMappings = urlMappings.toArray(String[]::new);
	}

	@Override
	protected Dynamic addRegistration(String description, ServletContext servletContext) {
		return servletContext.addServlet(getName(), this.servlet);
	}

	@Override
	protected void configure(Dynamic registration) {
		super.configure(registration);
		if (!ObjectUtils.isEmpty(this.urlMappings)) {
			registration.addMapping(this.urlMappings);
		}
	}

	@Override
	protected String getDescription() {
		return getName();
	}

	private String getName() {
		return getOrDeduceName(this.servlet);
	}

}
