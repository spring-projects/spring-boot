/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} to configure the defaults used
 * for all imperative and reactive HTTP clients.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@ConfigurationProperties("spring.http.clients")
public class HttpClientsProperties extends HttpClientSettingsProperties {

}
