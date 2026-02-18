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

package smoketest.restclient;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jackson.deserialization.fail-on-null-for-primitives=false")
class SampleRestClientApplicationGh49223Tests {

	@Autowired
	private RestClient.Builder restClientBuilder;

	@Test
	void applicationStarts() {
		RestClient restClient = this.restClientBuilder.build();
		List<?> messageConverters = (List<?>) ReflectionTestUtils.getField(restClient, "messageConverters");
		assertThat(messageConverters).isNotNull();
		JacksonJsonHttpMessageConverter jacksonConverter = (JacksonJsonHttpMessageConverter) messageConverters.stream()
			.filter((converter) -> converter instanceof JacksonJsonHttpMessageConverter)
			.findFirst()
			.orElseThrow();
		assertThat(jacksonConverter.getMapper().isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES))
			.isFalse();
	}

}
