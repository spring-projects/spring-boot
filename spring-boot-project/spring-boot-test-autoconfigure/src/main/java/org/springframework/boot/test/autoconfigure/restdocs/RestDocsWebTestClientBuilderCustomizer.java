/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.restdocs;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.test.autoconfigure.web.reactive.WebTestClientBuilderCustomizer;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentationConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StringUtils;

/**
 * A customizer that configures Spring REST Docs with WebTestClient.
 *
 * @author Eddú Meléndez
 * @author Roman Zaynetdinov
 */
class RestDocsWebTestClientBuilderCustomizer implements InitializingBean, WebTestClientBuilderCustomizer {

	private final WebTestClientRestDocumentationConfigurer delegate;

	private String uriScheme;

	private String uriHost;

	private Integer uriPort;

	RestDocsWebTestClientBuilderCustomizer(WebTestClientRestDocumentationConfigurer delegate) {
		this.delegate = delegate;
	}

	public String getUriScheme() {
		return this.uriScheme;
	}

	public void setUriScheme(String uriScheme) {
		this.uriScheme = uriScheme;
	}

	public String getUriHost() {
		return this.uriHost;
	}

	public void setUriHost(String uriHost) {
		this.uriHost = uriHost;
	}

	public Integer getUriPort() {
		return this.uriPort;
	}

	public void setUriPort(Integer uriPort) {
		this.uriPort = uriPort;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	@Override
	public void customize(WebTestClient.Builder builder) {
		if (StringUtils.hasText(this.uriScheme) && StringUtils.hasText(this.uriHost)) {
			String baseUrl = this.uriScheme + "://" + this.uriHost;

			if (this.uriPort == 80 && this.uriScheme.equals("http")) {
				// Don't add default port
			} else if (this.uriPort == 443 && this.uriScheme.equals("https")) {
				// Don't add default port
			} else if (this.uriPort != null) {
				baseUrl += ":" + this.uriPort;
			}

			builder.baseUrl(baseUrl);
		}
		builder.filter(this.delegate);
	}
}
