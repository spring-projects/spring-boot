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

package org.springframework.boot.autoconfigure.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryProvider;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Simple driver for testing.
 *
 * @author Mark Paluch
 * @author Andy Wilkinson
 */
public class SimpleConnectionFactoryProvider implements ConnectionFactoryProvider {

	@Override
	public ConnectionFactory create(ConnectionFactoryOptions connectionFactoryOptions) {
		return new SimpleTestConnectionFactory();
	}

	@Override
	public boolean supports(ConnectionFactoryOptions connectionFactoryOptions) {
		return connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.DRIVER).equals("simple");
	}

	@Override
	public String getDriver() {
		return "simple";
	}

	public static class SimpleTestConnectionFactory implements ConnectionFactory {

		@Override
		public Publisher<? extends Connection> create() {
			return Mono.error(new UnsupportedOperationException());
		}

		@Override
		public ConnectionFactoryMetadata getMetadata() {
			return SimpleConnectionFactoryProvider.class::getName;
		}

	}

}
