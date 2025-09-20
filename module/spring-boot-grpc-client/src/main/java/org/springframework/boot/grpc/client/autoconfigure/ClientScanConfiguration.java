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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.grpc.client.autoconfigure.ClientScanConfiguration.DefaultGrpcClientRegistrations;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.ChannelConfig;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.grpc.client.AbstractGrpcClientRegistrar;
import org.springframework.grpc.client.GrpcClientFactory;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(GrpcClientFactory.class)
@Import(DefaultGrpcClientRegistrations.class)
public class ClientScanConfiguration {

	static class DefaultGrpcClientRegistrations extends AbstractGrpcClientRegistrar
			implements EnvironmentAware, BeanFactoryAware {

		private @Nullable Environment environment;

		private @Nullable BeanFactory beanFactory;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		protected GrpcClientRegistrationSpec[] collect(AnnotationMetadata meta) {
			Assert.notNull(this.environment, "Environment must not be null");
			Assert.notNull(this.beanFactory, "BeanFactory must not be null");
			Binder binder = Binder.get(this.environment);
			boolean hasDefaultChannel = binder.bind("spring.grpc.client.default-channel", ChannelConfig.class)
				.isBound();
			if (hasDefaultChannel) {
				List<String> packages = new ArrayList<>();
				if (AutoConfigurationPackages.has(this.beanFactory)) {
					packages.addAll(AutoConfigurationPackages.get(this.beanFactory));
				}
				GrpcClientProperties props = binder.bind("spring.grpc.client", GrpcClientProperties.class)
					.orElseGet(GrpcClientProperties::new);

				return new GrpcClientRegistrationSpec[] { GrpcClientRegistrationSpec.of("default")
					.factory(props.getDefaultStubFactory())
					.packages(packages.toArray(new String[0])) };
			}
			return new GrpcClientRegistrationSpec[0];
		}

	}

}
