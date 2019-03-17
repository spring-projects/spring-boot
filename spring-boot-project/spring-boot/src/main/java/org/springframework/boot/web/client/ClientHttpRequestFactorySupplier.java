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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ClassUtils;

/**
 * A supplier for {@link ClientHttpRequestFactory} that detects the preferred candidate
 * based on the available implementations on the classpath.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class ClientHttpRequestFactorySupplier
		implements Supplier<ClientHttpRequestFactory> {

	private static final Map<String, String> REQUEST_FACTORY_CANDIDATES;

	static {
		Map<String, String> candidates = new LinkedHashMap<>();
		candidates.put("org.apache.http.client.HttpClient",
				"org.springframework.http.client.HttpComponentsClientHttpRequestFactory");
		candidates.put("okhttp3.OkHttpClient",
				"org.springframework.http.client.OkHttp3ClientHttpRequestFactory");
		REQUEST_FACTORY_CANDIDATES = Collections.unmodifiableMap(candidates);
	}

	@Override
	public ClientHttpRequestFactory get() {
		for (Map.Entry<String, String> candidate : REQUEST_FACTORY_CANDIDATES
				.entrySet()) {
			ClassLoader classLoader = getClass().getClassLoader();
			if (ClassUtils.isPresent(candidate.getKey(), classLoader)) {
				Class<?> factoryClass = ClassUtils.resolveClassName(candidate.getValue(),
						classLoader);
				return (ClientHttpRequestFactory) BeanUtils
						.instantiateClass(factoryClass);
			}
		}
		return new SimpleClientHttpRequestFactory();
	}

}
