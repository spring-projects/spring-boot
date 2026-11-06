/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure.reactive;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerPropertyAdapters;
import org.springframework.boot.web.server.reactive.ConfigurableReactiveWebServerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yunkun Huang
 * @author Scott Frederick
 * @since 4.0.0
 */
public class ReactiveWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory>, Ordered {

	private final @Nullable Environment environment;

	private final ServerProperties serverProperties;

	private final @Nullable SslBundles sslBundles;

	/**
	 * Create a new {@link ReactiveWebServerFactoryCustomizer} instance.
	 * @param serverProperties the server properties
	 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
	 * {@link #ReactiveWebServerFactoryCustomizer(Environment, ServerProperties, SslBundles)}
	 */
	@Deprecated(since = "4.2.0", forRemoval = true)
	public ReactiveWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this(serverProperties, null);
	}

	/**
	 * Create a new {@link ReactiveWebServerFactoryCustomizer} instance.
	 * @param serverProperties the server properties
	 * @param sslBundles the SSL bundles
	 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
	 * {@link #ReactiveWebServerFactoryCustomizer(Environment, ServerProperties, SslBundles)}
	 */
	@Deprecated(since = "4.2.0", forRemoval = true)
	public ReactiveWebServerFactoryCustomizer(ServerProperties serverProperties, @Nullable SslBundles sslBundles) {
		this.environment = null;
		this.serverProperties = serverProperties;
		this.sslBundles = sslBundles;
	}

	/**
	 * Create a new {@link ReactiveWebServerFactoryCustomizer} instance.
	 * @param environment the environment
	 * @param serverProperties the server properties
	 * @param sslBundles the SSL bundles
	 * @since 4.2.0
	 */
	public ReactiveWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties,
			@Nullable SslBundles sslBundles) {
		this.environment = null;
		this.serverProperties = serverProperties;
		this.sslBundles = sslBundles;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableReactiveWebServerFactory factory) {
		PropertyMapper map = PropertyMapper.get();
		map.from(this.serverProperties::getPort).to(factory::setPort);
		map.from(this.serverProperties::getAddress).to(factory::setAddress);
		map.from(this.serverProperties::getSsl).to(factory::setSsl);
		map.from(this.serverProperties::getCompression).to(factory::setCompression);
		map.from(this.serverProperties::getHttp2).to(factory::setHttp2);
		map.from(this.serverProperties)
			.as(ServerPropertyAdapters.webServerForwardedRequestHeaders(this.environment))
			.to(factory::setForwardedRequestHeaders);
		map.from(this.serverProperties.getShutdown()).to(factory::setShutdown);
		map.from(() -> this.sslBundles).to(factory::setSslBundles);
	}

}
