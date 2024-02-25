/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.restdocs;

import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentationConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StringUtils;

/**
 * A {@link WebTestClientBuilderCustomizer} that configures Spring REST Docs.
 *
 * @author Roman Zaynetdinov
 * @author Andy Wilkinson
 */
class RestDocsWebTestClientBuilderCustomizer implements WebTestClientBuilderCustomizer {

	private final RestDocsProperties properties;

	private final WebTestClientRestDocumentationConfigurer delegate;

	/**
     * Constructs a new RestDocsWebTestClientBuilderCustomizer with the specified properties and delegate.
     * 
     * @param properties the RestDocsProperties to be used
     * @param delegate the WebTestClientRestDocumentationConfigurer to be used
     */
    RestDocsWebTestClientBuilderCustomizer(RestDocsProperties properties,
			WebTestClientRestDocumentationConfigurer delegate) {
		this.properties = properties;
		this.delegate = delegate;
	}

	/**
     * Customize the WebTestClient.Builder by setting the base URL and adding a filter.
     * 
     * @param builder the WebTestClient.Builder to customize
     */
    @Override
	public void customize(WebTestClient.Builder builder) {
		customizeBaseUrl(builder);
		builder.filter(this.delegate);
	}

	/**
     * Customizes the base URL for the WebTestClient builder.
     * 
     * @param builder the WebTestClient builder to customize
     */
    private void customizeBaseUrl(WebTestClient.Builder builder) {
		String scheme = this.properties.getUriScheme();
		String host = this.properties.getUriHost();
		String baseUrl = (StringUtils.hasText(scheme) ? scheme : "http") + "://"
				+ (StringUtils.hasText(host) ? host : "localhost");
		Integer port = this.properties.getUriPort();
		if (!isStandardPort(scheme, port)) {
			baseUrl += ":" + port;
		}
		builder.baseUrl(baseUrl);
	}

	/**
     * Checks if the given port is a standard port for the specified scheme.
     * 
     * @param scheme the scheme (e.g., "http" or "https")
     * @param port the port number
     * @return true if the port is a standard port for the scheme, false otherwise
     */
    private boolean isStandardPort(String scheme, Integer port) {
		if (port == null) {
			return true;
		}
		return ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
	}

}
