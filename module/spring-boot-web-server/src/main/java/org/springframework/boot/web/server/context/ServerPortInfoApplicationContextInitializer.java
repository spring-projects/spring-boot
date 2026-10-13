/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServer;
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
 * ports that {@link WebServer} servers are actually listening on. The property
 * {@literal "local.server.port"} can be injected directly into tests using
 * {@link Value @Value} or obtained through the {@link Environment}.
 * <p>
 * If the {@link WebServerInitializedEvent} has a
 * {@link WebServerApplicationContext#getServerNamespace() server namespace}, it will be
 * used to construct the property name. For example, the "management" actuator context
 * will have the property name {@literal "local.management.port"}.
 * <p>
 * Properties are automatically propagated up to any parent context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ServerPortInfoApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<WebServerInitializedEvent> {

	private static final String PROPERTY_SOURCE_NAME = "server.ports";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addApplicationListener(this);
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		String propertyName = "local." + getName(event.getApplicationContext()) + ".port";
		setPortProperty(event.getApplicationContext(), propertyName, event.getWebServer().getPort());
	}

	private String getName(WebServerApplicationContext context) {
		String name = context.getServerNamespace();
		return StringUtils.hasText(name) ? name : "server";
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
