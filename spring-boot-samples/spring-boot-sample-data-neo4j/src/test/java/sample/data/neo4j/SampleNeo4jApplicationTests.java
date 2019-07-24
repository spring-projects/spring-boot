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

package sample.data.neo4j;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;

import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleNeo4jApplication}.
 *
 * @author Stephane Nicoll
 */
public class SampleNeo4jApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testDefaultSettings() {
		try {
			SampleNeo4jApplication.main(new String[0]);
		}
		catch (Exception ex) {
			if (!neo4jServerRunning(ex)) {
				return;
			}
		}
		String output = this.outputCapture.toString();
		assertThat(output).contains("firstName='Alice', lastName='Smith'");
	}

	private boolean neo4jServerRunning(Throwable ex) {
		if (ex instanceof ServiceUnavailableException) {
			return false;
		}
		return (ex.getCause() == null || neo4jServerRunning(ex.getCause()));
	}

}
