/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.mixed;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JsonTest JSON Tests} for {@link SampleJackson2MixedApplication}.
 *
 * @author Andy Wilkinson
 */
@JsonTest
class SampleJackson2MixedApplicationJsonTests {

	@Autowired
	@SuppressWarnings({ "deprecation", "removal" })
	org.springframework.boot.test.json.Jackson2Tester<Pojo> jackson2Tester;

	@Autowired
	JacksonTester<Pojo> jacksonTester;

	@Test
	void jackson2TesterIsInitialized() {
		assertThat(this.jackson2Tester).isNotNull();
	}

	@Test
	void jacksonTesterIsInitialized() {
		assertThat(this.jacksonTester).isNotNull();
	}

	static class Pojo {

	}

}
