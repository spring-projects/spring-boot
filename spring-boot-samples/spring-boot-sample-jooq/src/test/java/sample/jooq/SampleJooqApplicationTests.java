/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.jooq;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SampleJooqApplication}.
 */
public class SampleJooqApplicationTests {

	private static final String[] NO_ARGS = {};

	@Rule
	public OutputCapture out = new OutputCapture();

	@Test
	public void outputResults() throws Exception {
		SampleJooqApplication.main(NO_ARGS);
		assertThat(this.out.toString()).contains("jOOQ Fetch 1 Greg Turnquest");
		assertThat(this.out.toString()).contains("jOOQ Fetch 2 Craig Walls");
		assertThat(this.out.toString())
				.contains("jOOQ SQL " + "[Learning Spring Boot : Greg Turnquest, "
						+ "Spring Boot in Action : Craig Walls]");
	}

}
