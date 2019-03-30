/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.data.elasticsearch;

import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleElasticsearchApplication}.
 *
 * @author Artur Konczak
 */
public class SampleElasticsearchApplicationTests {

	@Rule
	public final OutputCapture output = new OutputCapture();

	@Test
	public void testDefaultSettings() {
		try {
			new SpringApplicationBuilder(SampleElasticsearchApplication.class).run();
		}
		catch (Exception ex) {
			if (!elasticsearchRunning(ex)) {
				return;
			}
			throw ex;
		}
		assertThat(this.output.toString())
				.contains("firstName='Alice', lastName='Smith'");
	}

	private boolean elasticsearchRunning(Exception ex) {
		Throwable candidate = ex;
		while (candidate != null) {
			if (candidate instanceof NoNodeAvailableException) {
				return false;
			}
			candidate = candidate.getCause();
		}
		return true;
	}

}
