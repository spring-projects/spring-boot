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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.ChannelConfig;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.grpc.client.AbstractGrpcClientRegistrar;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;

/**
 * Default implementation of {@link AbstractGrpcClientRegistrar} that registers client
 * bean definitions for the default configured gRPC channel if the
 * {@code spring.grpc.client.default-channel} property is set.
 *
 * @author Dave Syer
 * @author Chris Bono
 */
class DefaultGrpcClientRegistrations extends AbstractGrpcClientRegistrar {

	private final Environment environment;

	private final BeanFactory beanFactory;

	DefaultGrpcClientRegistrations(Environment environment, BeanFactory beanFactory) {
		this.environment = environment;
		this.beanFactory = beanFactory;
	}

	@Override
	protected GrpcClientRegistrationSpec[] collect(AnnotationMetadata meta) {
		Binder binder = Binder.get(this.environment);
		boolean hasDefaultChannel = binder.bind("spring.grpc.client.default-channel", ChannelConfig.class).isBound();
		if (hasDefaultChannel) {
			List<String> packages = new ArrayList<>();
			if (AutoConfigurationPackages.has(this.beanFactory)) {
				packages.addAll(AutoConfigurationPackages.get(this.beanFactory));
			}
			GrpcClientProperties clientProperties = binder.bind("spring.grpc.client", GrpcClientProperties.class)
				.orElseGet(GrpcClientProperties::new);
			return new GrpcClientRegistrationSpec[] { GrpcClientRegistrationSpec.of("default")
				.factory(clientProperties.getDefaultStubFactory())
				.packages(packages.toArray(new String[0])) };
		}
		return new GrpcClientRegistrationSpec[0];
	}

}
