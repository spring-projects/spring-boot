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

package org.springframework.boot.resttestclient.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate.HttpClientOption;
import org.springframework.boot.test.http.client.BaseUrlUriBuilderFactory;
import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.boot.test.http.server.BaseUrlProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for {@link TestRestTemplate}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
public final class TestRestTemplateAutoConfiguration {

	private static final HttpClientOption[] DEFAULT_OPTIONS = {};

	private static final HttpClientOption[] SSL_OPTIONS = { HttpClientOption.SSL };

	@Bean(name = "org.springframework.boot.resttestclient.TestRestTemplate")
	@ConditionalOnMissingBean
	TestRestTemplate testRestTemplate(ObjectProvider<RestTemplateBuilder> builderProvider,
			ApplicationContext applicationContext) {
		RestTemplateBuilder builder = builderProvider.getIfAvailable(RestTemplateBuilder::new);
		BaseUrl baseUrl = new BaseUrlProviders(applicationContext).getBaseUrl(BaseUrl.LOCALHOST);
		TestRestTemplate template = new TestRestTemplate(builder, null, null,
				baseUrl.isHttps() ? SSL_OPTIONS : DEFAULT_OPTIONS);
		template.setUriTemplateHandler(BaseUrlUriBuilderFactory.get(baseUrl));
		return template;
	}

}
