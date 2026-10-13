/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;

/**
 * Callback interface that can be used to customize the {@link ProtocolHandler} on the
 * {@link Connector}.
 *
 * @param <T> specified type for customization based on {@link ProtocolHandler}
 * @author Pascal Zwick
 * @since 4.0.0
 * @see ConfigurableTomcatWebServerFactory
 */
@FunctionalInterface
public interface TomcatProtocolHandlerCustomizer<T extends ProtocolHandler> {

	/**
	 * Customize the protocol handler.
	 * @param protocolHandler the protocol handler to customize
	 */
	void customize(T protocolHandler);

}
