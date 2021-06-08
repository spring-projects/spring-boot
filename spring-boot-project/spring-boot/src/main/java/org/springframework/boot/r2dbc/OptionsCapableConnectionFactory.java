/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Wrapped;
import org.reactivestreams.Publisher;

/**
 * {@link ConnectionFactory} capable of providing access to the
 * {@link ConnectionFactoryOptions} from which it was built.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class OptionsCapableConnectionFactory implements Wrapped<ConnectionFactory>, ConnectionFactory {

	private final ConnectionFactoryOptions options;

	private final ConnectionFactory delegate;

	/**
	 * Create a new {@code OptionsCapableConnectionFactory} that will provide access to
	 * the given {@code options} that were used to build the given {@code delegate}
	 * {@link ConnectionFactory}.
	 * @param options the options from which the connection factory was built
	 * @param delegate the delegate connection factory that was built with options
	 */
	public OptionsCapableConnectionFactory(ConnectionFactoryOptions options, ConnectionFactory delegate) {
		this.options = options;
		this.delegate = delegate;
	}

	public ConnectionFactoryOptions getOptions() {
		return this.options;
	}

	@Override
	public Publisher<? extends Connection> create() {
		return this.delegate.create();
	}

	@Override
	public ConnectionFactoryMetadata getMetadata() {
		return this.delegate.getMetadata();
	}

	@Override
	public ConnectionFactory unwrap() {
		return this.delegate;
	}

	/**
	 * Returns, if possible, an {@code OptionsCapableConnectionFactory} by unwrapping the
	 * given {@code connectionFactory} as necessary. If the given
	 * {@code connectionFactory} does not wrap an {@code OptionsCapableConnectionFactory}
	 * and is not itself an {@code OptionsCapableConnectionFactory}, {@code null} is
	 * returned.
	 * @param connectionFactory the connection factory to unwrap
	 * @return the {@code OptionsCapableConnectionFactory} or {@code null}
	 * @since 2.5.1
	 */
	public static OptionsCapableConnectionFactory unwrapFrom(ConnectionFactory connectionFactory) {
		if (connectionFactory instanceof OptionsCapableConnectionFactory) {
			return (OptionsCapableConnectionFactory) connectionFactory;
		}
		if (connectionFactory instanceof Wrapped) {
			Object unwrapped = ((Wrapped<?>) connectionFactory).unwrap();
			if (unwrapped instanceof ConnectionFactory) {
				return unwrapFrom((ConnectionFactory) unwrapped);
			}
		}
		return null;

	}

}
