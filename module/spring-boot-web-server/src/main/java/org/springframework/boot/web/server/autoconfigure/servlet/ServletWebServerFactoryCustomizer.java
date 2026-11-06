/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure.servlet;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerPropertyAdapters;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.boot.web.server.servlet.WebListenerRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} and
 * {@link WebListenerRegistrar WebListenerRegistrars} to servlet web servers.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Olivier Lamy
 * @author Yunkun Huang
 * @author Scott Frederick
 * @author Lasse Wulff
 * @since 4.0.0
 */
public class ServletWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, Ordered {

	private final @Nullable Environment environment;

	private final ServerProperties serverProperties;

	private final List<WebListenerRegistrar> webListenerRegistrars;

	private final List<CookieSameSiteSupplier> cookieSameSiteSuppliers;

	private final @Nullable SslBundles sslBundles;

	/**
	 * Create a new {@link ServletWebServerFactoryCustomizer} instance.
	 * @param serverProperties the server properties
	 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
	 * {@link #ServletWebServerFactoryCustomizer(Environment, ServerProperties, List, List, SslBundles)}
	 */
	@Deprecated(since = "4.2.0", forRemoval = true)
	public ServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this(serverProperties, Collections.emptyList(), Collections.emptyList(), null);
	}

	/**
	 * Create a new {@link ServletWebServerFactoryCustomizer} instance.
	 * @param serverProperties the server properties
	 * @param webListenerRegistrars the web listener registrars
	 * @param cookieSameSiteSuppliers the cookie same-site suppliers
	 * @param sslBundles the SSL bundles
	 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
	 * {@link #ServletWebServerFactoryCustomizer(Environment, ServerProperties, List, List, SslBundles)}
	 */
	@Deprecated(since = "4.2.0", forRemoval = true)
	public ServletWebServerFactoryCustomizer(ServerProperties serverProperties,
			List<WebListenerRegistrar> webListenerRegistrars, List<CookieSameSiteSupplier> cookieSameSiteSuppliers,
			@Nullable SslBundles sslBundles) {
		this.environment = null;
		this.serverProperties = serverProperties;
		this.webListenerRegistrars = webListenerRegistrars;
		this.cookieSameSiteSuppliers = cookieSameSiteSuppliers;
		this.sslBundles = sslBundles;
	}

	/**
	 * Create a new {@link ServletWebServerFactoryCustomizer} instance.
	 * @param environment the environment.
	 * @param serverProperties the server properties
	 * @param webListenerRegistrars the web listener registrars
	 * @param cookieSameSiteSuppliers the cookie same-site suppliers
	 * @param sslBundles the SSL bundles
	 * @since 4.2.0
	 */
	public ServletWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties,
			List<WebListenerRegistrar> webListenerRegistrars, List<CookieSameSiteSupplier> cookieSameSiteSuppliers,
			@Nullable SslBundles sslBundles) {
		this.environment = environment;
		this.serverProperties = serverProperties;
		this.webListenerRegistrars = webListenerRegistrars;
		this.cookieSameSiteSuppliers = cookieSameSiteSuppliers;
		this.sslBundles = sslBundles;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableServletWebServerFactory factory) {
		PropertyMapper map = PropertyMapper.get();
		map.from(this.serverProperties::getPort).to(factory::setPort);
		map.from(this.serverProperties::getAddress).to(factory::setAddress);
		map.from(this.serverProperties.getServlet()::getContextPath).to(factory::setContextPath);
		map.from(this.serverProperties.getServlet()::getApplicationDisplayName).to(factory::setDisplayName);
		map.from(this.serverProperties.getServlet()::isRegisterDefaultServlet).to(factory::setRegisterDefaultServlet);
		map.from(this.serverProperties.getServlet()::getSession).to(factory::setSession);
		map.from(this.serverProperties::getSsl).to(factory::setSsl);
		map.from(this.serverProperties.getServlet()::getJsp).to(factory::setJsp);
		map.from(this.serverProperties::getCompression).to(factory::setCompression);
		map.from(this.serverProperties::getHttp2).to(factory::setHttp2);
		map.from(this.serverProperties::getServerHeader).to(factory::setServerHeader);
		map.from(this.serverProperties.getServlet()::getContextParameters).to(factory::setInitParameters);
		map.from(this.serverProperties)
			.as(ServerPropertyAdapters.webServerForwardedRequestHeaders(this.environment))
			.to(factory::setForwardedRequestHeaders);
		map.from(this.serverProperties.getShutdown()).to(factory::setShutdown);
		map.from(() -> this.sslBundles).to(factory::setSslBundles);
		map.from(() -> this.cookieSameSiteSuppliers)
			.whenNot(CollectionUtils::isEmpty)
			.to(factory::setCookieSameSiteSuppliers);
		map.from(this.serverProperties::getMimeMappings).to(factory::addMimeMappings);
		map.from(this.serverProperties.getServlet().getEncoding()::getMapping).to(factory::setLocaleCharsetMappings);
		this.webListenerRegistrars.forEach((registrar) -> registrar.register(factory));
	}

}
