/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient;

import org.springframework.web.client.RestTemplate;

/**
 * Callback interface that can be used to customize a {@link RestTemplate}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see RestTemplateBuilder
 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of {@link RestClientCustomizer}
 */
@FunctionalInterface
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
public interface RestTemplateCustomizer {

	/**
	 * Callback to customize a {@link RestTemplate} instance.
	 * @param restTemplate the template to customize
	 */
	void customize(RestTemplate restTemplate);

}
