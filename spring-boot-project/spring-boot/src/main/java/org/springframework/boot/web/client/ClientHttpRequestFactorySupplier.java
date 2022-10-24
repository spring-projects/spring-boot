/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.function.Supplier;

import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * A supplier for {@link ClientHttpRequestFactory} that detects the preferred candidate
 * based on the available implementations on the classpath.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @since 2.1.0
 * @deprecated since 3.0.0 for removal in 3.2.0 in favor of
 * {@link ClientHttpRequestFactories}
 */
@Deprecated(since = "3.0.0", forRemoval = true)
public class ClientHttpRequestFactorySupplier implements Supplier<ClientHttpRequestFactory> {

	@Override
	public ClientHttpRequestFactory get() {
		return ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS);
	}

}
