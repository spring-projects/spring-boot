/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.r2dbc;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.r2dbc.SimpleConnectionFactoryProvider.SimpleTestConnectionFactory;
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

	private ConnectionFactory unwrapIfNecessary(ConnectionFactory connectionFactory) {
		if (connectionFactory instanceof ConnectionPool) {
			return ((ConnectionPool) connectionFactory).unwrap();
		}
		return connectionFactory;
	}

}
