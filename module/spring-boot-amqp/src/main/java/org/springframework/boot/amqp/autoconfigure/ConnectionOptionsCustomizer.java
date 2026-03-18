/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure;

import org.apache.qpid.protonj2.client.ConnectionOptions;

import org.springframework.amqp.client.AmqpConnectionFactory;

/**
 * Callback interface for customizing {@link ConnectionOptions} on the auto-configured
 * {@link AmqpConnectionFactory}.
 *
 * @author Stephane Nicoll
 * @since 4.1.0
 */
@FunctionalInterface
public interface ConnectionOptionsCustomizer {

	/**
	 * Customize the {@link ConnectionOptions}.
	 * @param connectionOptions the connection options to customize
	 */
	void customize(ConnectionOptions connectionOptions);

}
