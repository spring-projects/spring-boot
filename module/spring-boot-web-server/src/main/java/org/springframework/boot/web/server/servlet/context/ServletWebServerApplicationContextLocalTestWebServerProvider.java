/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.boot.test.http.server.LocalTestWebServer.BaseUriDetails;
import org.springframework.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.context.ApplicationContext;

/**
 * {@link LocalTestWebServer} provider for a {@link ServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class ServletWebServerApplicationContextLocalTestWebServerProvider implements LocalTestWebServer.Provider {

	private final @Nullable ServletWebServerApplicationContext context;

	ServletWebServerApplicationContextLocalTestWebServerProvider(ApplicationContext context) {
		this.context = getWebServerApplicationContextIfPossible(context);
	}

	static @Nullable ServletWebServerApplicationContext getWebServerApplicationContextIfPossible(
			ApplicationContext context) {
		try {
			return (ServletWebServerApplicationContext) context;
		}
		catch (NoClassDefFoundError | ClassCastException ex) {
			return null;
		}
	}

	@Override
	public @Nullable LocalTestWebServer getLocalTestWebServer() {
		if (this.context == null) {
			return null;
		}
		return LocalTestWebServer.of((isSslEnabled(this.context)) ? Scheme.HTTPS : Scheme.HTTP, () -> {
			int port = this.context.getEnvironment().getProperty("local.server.port", Integer.class, 8080);
			String path = this.context.getEnvironment().getProperty("server.servlet.context-path", "");
			return new BaseUriDetails(port, path);
		});
	}

	private boolean isSslEnabled(ServletWebServerApplicationContext context) {
		try {
			AbstractConfigurableWebServerFactory webServerFactory = context
				.getBean(AbstractConfigurableWebServerFactory.class);
			return webServerFactory.getSsl() != null && webServerFactory.getSsl().isEnabled();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

}
