/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.only;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.ObjectContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleJackson2OnlyApplication} using {@link JsonTest @JsonTest}.
 *
 * @author Andy Wilkinson
 */
@JsonTest
@SuppressWarnings({ "deprecation", "removal" })
class SampleJackson2OnlyApplicationJsonTests {

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	org.springframework.boot.test.json.Jackson2Tester<JsonPojo> tester;

	@Test
	void objectMapperIsInjected() {
		assertThat(this.objectMapper).isNotNull();
	}

	@Test
	void jacksonTesterWorks() throws IOException {
		ObjectContent<JsonPojo> pojo = this.tester.parse("{\"alpha\":\"a\",\"bravo\":\"b\"}");
		assertThat(pojo).extracting(JsonPojo::getAlpha).isEqualTo("a");
		assertThat(pojo).extracting(JsonPojo::getBravo).isEqualTo("b");
	}

}
