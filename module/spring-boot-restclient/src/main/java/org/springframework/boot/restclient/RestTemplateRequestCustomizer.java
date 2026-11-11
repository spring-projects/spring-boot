/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient;

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
 * @since 4.0.0
 * @see RestTemplateBuilder
 * @see ClientHttpRequestInitializer
 * @deprecated since 4.2.0 for removal in 4.4.0 with no replacement
 */
@FunctionalInterface
@Deprecated(since = "4.2.0", forRemoval = true)
public interface RestTemplateRequestCustomizer<T extends ClientHttpRequest> {

	/**
	 * Customize the specified {@link ClientHttpRequest}.
	 * @param request the request to customize
	 */
	void customize(T request);

}
