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

package org.springframework.boot.restdocs.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.restdocs.test.autoconfigure.RestDocsProperties.Uri;
import org.springframework.boot.webtestclient.WebTestClientBuilderCustomizer;
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

	RestDocsWebTestClientBuilderCustomizer(RestDocsProperties properties,
			WebTestClientRestDocumentationConfigurer delegate) {
		this.properties = properties;
		this.delegate = delegate;
	}

	@Override
	public void customize(WebTestClient.Builder builder) {
		customizeBaseUrl(builder);
		builder.filter(this.delegate);
	}

	private void customizeBaseUrl(WebTestClient.Builder builder) {
		Uri uri = this.properties.getUri();
		String scheme = uri.getScheme();
		String host = uri.getHost();
		String baseUrl = (StringUtils.hasText(scheme) ? scheme : "http") + "://"
				+ (StringUtils.hasText(host) ? host : "localhost");
		Integer port = uri.getPort();
		if (!isStandardPort(scheme, port)) {
			baseUrl += ":" + port;
		}
		builder.baseUrl(baseUrl);
	}

	private boolean isStandardPort(@Nullable String scheme, @Nullable Integer port) {
		if (port == null) {
			return true;
		}
		return ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
	}

}
