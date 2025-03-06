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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.util.function.Consumer;

import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Interface that can be used to {@link WebClient.Builder#apply apply} SSL configuration
 * to a {@link org.springframework.web.reactive.function.client.WebClient.Builder
 * WebClient.Builder}.
 * <p>
 * Typically used as follows: <pre class="code">
 * &#064;Bean
 * public MyBean myBean(WebClient.Builder webClientBuilder, WebClientSsl ssl) {
 *     WebClient webClient = webClientBuilder.apply(ssl.fromBundle("mybundle")).build();
 *     return new MyBean(webClient);
 * }
 * </pre> NOTE: Apply SSL configuration will replace any previously
 * {@link WebClient.Builder#clientConnector configured} {@link ClientHttpConnector}.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface WebClientSsl {

	/**
	 * Return a {@link Consumer} that will apply SSL configuration for the named
	 * {@link SslBundle} to a
	 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
	 * WebClient.Builder}.
	 * @param bundleName the name of the SSL bundle to apply
	 * @return a {@link Consumer} to apply the configuration
	 * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
	 */
	Consumer<WebClient.Builder> fromBundle(String bundleName) throws NoSuchSslBundleException;

	/**
	 * Return a {@link Consumer} that will apply SSL configuration for the
	 * {@link SslBundle} to a
	 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
	 * WebClient.Builder}.
	 * @param bundle the SSL bundle to apply
	 * @return a {@link Consumer} to apply the configuration
	 */
	Consumer<WebClient.Builder> fromBundle(SslBundle bundle);

}
