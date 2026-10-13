/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure.servlet;

import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties.Servlet;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.util.ObjectUtils;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link TomcatServerProperties} to Tomcat
 * web servers.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class TomcatServletWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>, Ordered {

	private final Servlet tomcatServletProperties;

	TomcatServletWebServerFactoryCustomizer(TomcatServerProperties tomcatProperties) {
		this.tomcatServletProperties = tomcatProperties.getServlet();
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(TomcatServletWebServerFactory factory) {
		if (!ObjectUtils.isEmpty(this.tomcatServletProperties.getAdditionalTldSkipPatterns())) {
			factory.getTldSkipPatterns().addAll(this.tomcatServletProperties.getAdditionalTldSkipPatterns());
		}
		customizeRedirectContextRoot(factory, this.tomcatServletProperties.isRedirectContextRoot());
		customizeUseRelativeRedirects(factory, this.tomcatServletProperties.isUseRelativeRedirects());
	}

	private void customizeRedirectContextRoot(ConfigurableTomcatWebServerFactory factory, boolean redirectContextRoot) {
		factory.addContextCustomizers((context) -> context.setMapperContextRootRedirectEnabled(redirectContextRoot));
	}

	private void customizeUseRelativeRedirects(ConfigurableTomcatWebServerFactory factory,
			boolean useRelativeRedirects) {
		factory.addContextCustomizers((context) -> context.setUseRelativeRedirects(useRelativeRedirects));
	}

}
