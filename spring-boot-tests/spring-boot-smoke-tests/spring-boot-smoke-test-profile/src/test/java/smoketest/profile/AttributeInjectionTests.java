/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.profile;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// gh-29169
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AttributeInjectionTests {

	@Autowired(required = false)
	private org.springframework.boot.web.servlet.error.ErrorAttributes errorAttributesServlet;

	@Autowired(required = false)
	private org.springframework.boot.web.reactive.error.ErrorAttributes errorAttributesReactive;

	@Test
	void contextLoads() {
		assertThat(this.errorAttributesServlet).isNull();
		assertThat(this.errorAttributesReactive).isNotNull();
	}

}
