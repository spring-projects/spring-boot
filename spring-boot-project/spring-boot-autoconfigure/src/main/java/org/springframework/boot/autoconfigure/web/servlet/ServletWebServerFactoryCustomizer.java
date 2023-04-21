/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.WebListenerRegistrar;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.core.Ordered;
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
 * @since 2.0.0
 */
public class ServletWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, Ordered {

	private final ServerProperties serverProperties;

	private final List<WebListenerRegistrar> webListenerRegistrars;

	private final List<CookieSameSiteSupplier> cookieSameSiteSuppliers;

	private final SslBundles sslBundles;

	public ServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this(serverProperties, Collections.emptyList());
	}

	public ServletWebServerFactoryCustomizer(ServerProperties serverProperties,
			List<WebListenerRegistrar> webListenerRegistrars) {
		this(serverProperties, webListenerRegistrars, null, null);
	}

	ServletWebServerFactoryCustomizer(ServerProperties serverProperties,
			List<WebListenerRegistrar> webListenerRegistrars, List<CookieSameSiteSupplier> cookieSameSiteSuppliers,
			SslBundles sslBundles) {
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
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
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
		map.from(this.serverProperties.getShutdown()).to(factory::setShutdown);
		map.from(() -> this.sslBundles).to(factory::setSslBundles);
		map.from(() -> this.cookieSameSiteSuppliers)
			.whenNot(CollectionUtils::isEmpty)
			.to(factory::setCookieSameSiteSuppliers);
		this.webListenerRegistrars.forEach((registrar) -> registrar.register(factory));
	}

}
