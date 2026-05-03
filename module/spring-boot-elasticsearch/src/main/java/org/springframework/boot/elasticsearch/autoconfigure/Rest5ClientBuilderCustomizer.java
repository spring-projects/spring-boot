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

package org.springframework.boot.elasticsearch.autoconfigure;

import java.util.function.Consumer;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link Rest5ClientBuilder} to fine-tune its auto-configuration before it creates a
 * {@link Rest5Client}.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface Rest5ClientBuilderCustomizer {

	/**
	 * Customize the {@link Rest5ClientBuilder}.
	 * <p>
	 * Possibly overrides customizations made with the {@code "spring.elasticsearch.rest"}
	 * configuration properties namespace. For more targeted changes, see:
	 * <ul>
	 * <li>{@link #customize(HttpAsyncClientBuilder)}</li>
	 * <li>{@link #customize(RequestConfig.Builder)}</li>
	 * <li>{@link #customize(PoolingAsyncClientConnectionManagerBuilder)}</li>
	 * <li>{@link #customize(ConnectionConfig.Builder)}</li>
	 * </ul>
	 * @param builder the builder to customize
	 */
	void customize(Rest5ClientBuilder builder);

	/**
	 * Customize the {@link HttpAsyncClientBuilder}. Unlike
	 * {@link Rest5ClientBuilder#setHttpClientConfigCallback(Consumer)}, implementing this
	 * method does not replace other customization of the HTTP client builder.
	 * @param httpAsyncClientBuilder the HTTP client builder
	 */
	default void customize(HttpAsyncClientBuilder httpAsyncClientBuilder) {
	}

	/**
	 * Customize the {@link org.apache.hc.client5.http.config.RequestConfig.Builder}.
	 * Unlike {@link Rest5ClientBuilder#setRequestConfigCallback(Consumer)}, implementing
	 * this method does not replace other customization of the request config builder.
	 * @param requestConfigBuilder the request config builder
	 */
	default void customize(RequestConfig.Builder requestConfigBuilder) {
	}

	/**
	 * Customize the {@link PoolingAsyncClientConnectionManagerBuilder}. Unlike
	 * {@link Rest5ClientBuilder#setConnectionManagerCallback(Consumer)}, implementing
	 * this method does not replace other customization of the connection manager builder.
	 * @param connectionManagerBuilder the connection manager builder
	 */
	default void customize(PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder) {
	}

	/**
	 * Customize the {@link org.apache.hc.client5.http.config.ConnectionConfig.Builder}.
	 * Unlike {@link Rest5ClientBuilder#setConnectionConfigCallback(Consumer)},
	 * implementing this method does not replace other customization of the connection
	 * config builder.
	 * @param connectionConfigBuilder the connection config builder
	 */
	default void customize(ConnectionConfig.Builder connectionConfigBuilder) {

	}

}
