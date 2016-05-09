/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that sets {@link Environment} properties for the
 * ports that {@link EmbeddedServletContainer} servers are actually listening on. The
 * property {@literal "local.server.port"} can be injected directly into tests using
 * {@link Value @Value} or obtained via the {@link Environment}.
 * <p>
 * If the {@link EmbeddedWebApplicationContext} has a
 * {@link EmbeddedWebApplicationContext#setNamespace(String) namespace} set, it will be
 * used to construct the property name. For example, the "management" actuator context
 * will have the property name {@literal "local.management.port"}.
 * <p>
 * Properties are automatically propagated up to any parent context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.4.0
 */
public class ServerPortInfoApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addApplicationListener(
				new ApplicationListener<EmbeddedServletContainerInitializedEvent>() {

					@Override
					public void onApplicationEvent(
							EmbeddedServletContainerInitializedEvent event) {
						ServerPortInfoApplicationContextInitializer.this
								.onApplicationEvent(event);
					}

				});
	}

	protected void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
		String propertyName = getPropertyName(event.getApplicationContext());
		setPortProperty(event.getApplicationContext(), propertyName,
				event.getEmbeddedServletContainer().getPort());
	}

	protected String getPropertyName(EmbeddedWebApplicationContext context) {
		String name = context.getNamespace();
		if (StringUtils.isEmpty(name)) {
			name = "server";
		}
		return "local." + name + ".port";
	}

	private void setPortProperty(ApplicationContext context, String propertyName,
			int port) {
		if (context instanceof ConfigurableApplicationContext) {
			setPortProperty(((ConfigurableApplicationContext) context).getEnvironment(),
					propertyName, port);
		}
		if (context.getParent() != null) {
			setPortProperty(context.getParent(), propertyName, port);
		}
	}

	@SuppressWarnings("unchecked")
	private void setPortProperty(ConfigurableEnvironment environment, String propertyName,
			int port) {
		MutablePropertySources sources = environment.getPropertySources();
		PropertySource<?> source = sources.get("server.ports");
		if (source == null) {
			source = new MapPropertySource("server.ports", new HashMap<String, Object>());
			sources.addFirst(source);
		}
		((Map<String, Object>) source.getSource()).put(propertyName, port);
	}

}
