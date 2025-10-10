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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for {@link HttpGraphQlTester}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(afterName = { "org.springframework.boot.webtestclient.WebTestClientAutoConfiguration",
		"org.springframework.boot.webmvc.test.autoconfigure.MockMvcAutoConfiguration" })
@ConditionalOnClass({ WebClient.class, WebTestClient.class, WebGraphQlTester.class })
public final class HttpGraphQlTesterAutoConfiguration {

	@Bean
	@ConditionalOnBean(WebTestClient.class)
	@ConditionalOnMissingBean
	HttpGraphQlTester webTestClientGraphQlTester(WebTestClient webTestClient, GraphQlProperties properties) {
		WebTestClient mutatedWebTestClient = webTestClient.mutate().baseUrl(properties.getHttp().getPath()).build();
		return HttpGraphQlTester.create(mutatedWebTestClient);
	}

}
