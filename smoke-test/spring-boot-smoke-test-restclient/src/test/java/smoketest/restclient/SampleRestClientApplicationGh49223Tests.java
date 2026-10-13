/*
 * Copyright 2012-present the original author or authors.
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
