/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.client;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.web.client.RestTemplate;

/**
 * Callback interface that can be used to customize the {@link ClientHttpRequest} sent
 * from a {@link RestTemplate}.
 *
 * @param <T> the {@link ClientHttpRequest} type
 * @author Ilya Lukyanovich
 * @author Phillip Webb
 * @since 2.2.0
 * @see RestTemplateBuilder
 * @see ClientHttpRequestInitializer
 */
@FunctionalInterface
public interface RestTemplateRequestCustomizer<T extends ClientHttpRequest> {

	/**
	 * Customize the specified {@link ClientHttpRequest}.
	 * @param request the request to customize
	 */
	void customize(T request);

}
