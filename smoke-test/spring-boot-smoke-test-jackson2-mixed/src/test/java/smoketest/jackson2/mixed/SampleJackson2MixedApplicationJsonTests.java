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
