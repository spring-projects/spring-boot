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

package org.springframework.boot.grpc.test.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.util.Assert;

/**
 * {@link ApplicationContextInitializer} implementation to start the management context on
 * a random port if the main server's port is 0 and the management context is expected on
 * a different port.
 *
 * @author Dave Syer
 * @author Chris Bono
 */
class ServerPortInfoApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<GrpcServerStartedEvent> {

	private static final String PROPERTY_SOURCE_NAME = "grpc.server.ports";

	private @Nullable ConfigurableApplicationContext applicationContext;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		applicationContext.addApplicationListener(this);
	}

	@Override
	public void onApplicationEvent(GrpcServerStartedEvent event) {
		if (event.getSource().getFactory() instanceof InProcessGrpcServerFactory) {
			return;
		}
		String propertyName = "local.grpc.port";
		Assert.notNull(this.applicationContext, "ApplicationContext must not be null");
		setPortProperty(this.applicationContext, propertyName, event.getPort());
	}

	private void setPortProperty(ApplicationContext context, String propertyName, int port) {
		if (context instanceof ConfigurableApplicationContext configurableContext) {
			setPortProperty(configurableContext.getEnvironment(), propertyName, port);
		}
		if (context.getParent() != null) {
			setPortProperty(context.getParent(), propertyName, port);
		}
	}

	@SuppressWarnings("unchecked")
	private void setPortProperty(ConfigurableEnvironment environment, String propertyName, int port) {
		MutablePropertySources sources = environment.getPropertySources();
		PropertySource<?> source = sources.get(PROPERTY_SOURCE_NAME);
		if (source == null) {
			source = new MapPropertySource(PROPERTY_SOURCE_NAME, new HashMap<>());
			sources.addFirst(source);
		}
		((Map<String, Object>) source.getSource()).put(propertyName, port);
	}

}
