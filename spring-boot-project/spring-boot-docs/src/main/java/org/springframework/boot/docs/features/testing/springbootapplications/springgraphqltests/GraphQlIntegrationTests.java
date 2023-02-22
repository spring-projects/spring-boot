/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docs.features.testing.springbootapplications.springgraphqltests;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

@AutoConfigureHttpGraphQlTester
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class GraphQlIntegrationTests {

	@Test
	void shouldGreetWithSpecificName(@Autowired HttpGraphQlTester graphQlTester) {
		HttpGraphQlTester authenticatedTester = graphQlTester.mutate()
			.webTestClient((client) -> client.defaultHeaders((headers) -> headers.setBasicAuth("admin", "ilovespring")))
			.build();
		authenticatedTester.document("{ greeting(name: \"Alice\") } ")
			.execute()
			.path("greeting")
			.entity(String.class)
			.isEqualTo("Hello, Alice!");
	}

}
