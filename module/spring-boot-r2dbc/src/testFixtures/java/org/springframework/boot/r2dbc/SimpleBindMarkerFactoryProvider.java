/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;

import org.springframework.boot.r2dbc.SimpleConnectionFactoryProvider.SimpleTestConnectionFactory;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.r2dbc.core.binding.BindMarkersFactoryResolver.BindMarkerFactoryProvider;

/**
 * Simple {@link BindMarkerFactoryProvider} for {@link SimpleConnectionFactoryProvider}.
 *
 * @author Stephane Nicoll
 */
public class SimpleBindMarkerFactoryProvider implements BindMarkerFactoryProvider {

	@Override
	public BindMarkersFactory getBindMarkers(ConnectionFactory connectionFactory) {
		if (unwrapIfNecessary(connectionFactory) instanceof SimpleTestConnectionFactory) {
			return BindMarkersFactory.anonymous("?");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private ConnectionFactory unwrapIfNecessary(ConnectionFactory connectionFactory) {
		if (connectionFactory instanceof Wrapped) {
			return unwrapIfNecessary(((Wrapped<ConnectionFactory>) connectionFactory).unwrap());
		}
		return connectionFactory;
	}

}
