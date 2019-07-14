/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.docs.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringApplicationBuilderExample}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringApplicationBuilderExampleTests {

	@Test
	void contextHierarchyWithDisabledBanner(CapturedOutput output) {
		System.setProperty("spring.main.web-application-type", "none");
		try {
			new SpringApplicationBuilderExample().hierarchyWithDisabledBanner(new String[0]);
			assertThat(output).doesNotContain(":: Spring Boot ::");
		}
		finally {
			System.clearProperty("spring.main.web-application-type");
		}
	}

}
