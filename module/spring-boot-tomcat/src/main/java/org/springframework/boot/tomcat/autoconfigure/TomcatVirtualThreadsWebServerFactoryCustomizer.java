/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure;

import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;

import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * Activates {@link VirtualThreadExecutor} on {@link ProtocolHandler Tomcat's protocol
 * handler}.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public class TomcatVirtualThreadsWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, Ordered {

	@Override
	public void customize(ConfigurableTomcatWebServerFactory factory) {
		factory.addProtocolHandlerCustomizers(
				(protocolHandler) -> protocolHandler.setExecutor(new VirtualThreadExecutor("tomcat-handler-")));
	}

	@Override
	public int getOrder() {
		return TomcatWebServerFactoryCustomizer.ORDER + 1;
	}

}
