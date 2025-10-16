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

package org.springframework.boot.graphql.test.autoconfigure.tester;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties;
import org.springframework.boot.test.http.client.BaseUrlUriBuilderFactory;
import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.boot.test.http.server.BaseUrlProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for {@link HttpGraphQlTester}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.webtestclient.WebTestClientAutoConfiguration")
@ConditionalOnClass({ WebClient.class, WebTestClient.class, WebGraphQlTester.class })
@EnableConfigurationProperties(GraphQlProperties.class)
public final class HttpGraphQlTesterAutoConfiguration {

	@Bean
	@ConditionalOnBean(WebTestClient.class)
	@ConditionalOnMissingBean
	HttpGraphQlTester webTestClientGraphQlTester(ApplicationContext applicationContext, WebTestClient webTestClient,
			GraphQlProperties properties) {
		String graphQlPath = properties.getHttp().getPath();
		BaseUrl baseUrl = new BaseUrlProviders(applicationContext).getBaseUrl();
		WebTestClient graphQlWebTestClient = configureGraphQlWebTestClient(webTestClient, baseUrl, graphQlPath);
		return HttpGraphQlTester.create(graphQlWebTestClient);
	}

	private WebTestClient configureGraphQlWebTestClient(WebTestClient webTestClient, @Nullable BaseUrl baseUrl,
			String graphQlPath) {
		WebTestClient.Builder builder = webTestClient.mutate();
		if (baseUrl != null) {
			return builder.uriBuilderFactory(BaseUrlUriBuilderFactory.get(baseUrl.withPath(graphQlPath))).build();
		}
		return builder.baseUrl(graphQlPath).build();
	}

}
