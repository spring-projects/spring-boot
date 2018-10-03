/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.client;

import java.time.Duration;
import java.util.function.Consumer;

import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * A customizer to set properties on passed-in {@link ClientHttpRequestFactory} instances
 * as used by {@link RestTemplateBuilder}.
 *
 * @author Brian Deacon
 * @since 2.1.0
 */
public interface RequestFactoryCustomizer extends Consumer<ClientHttpRequestFactory> {

	/**
	 * Specifies the read timeout that will be set on customized
	 * {@link ClientHttpRequestFactory} instances.
	 * @param readTimeout the read timeout
	 * @return the configured instance of the customizer
	 */
	RequestFactoryCustomizer readTimeout(Duration readTimeout);

	/**
	 * Specifies the connection timeout that will be set on customized
	 * {@link ClientHttpRequestFactory} instances.
	 * @param connectTimeout the connect timeout
	 * @return the configured instance of the customizer
	 */
	RequestFactoryCustomizer connectTimeout(Duration connectTimeout);

}
