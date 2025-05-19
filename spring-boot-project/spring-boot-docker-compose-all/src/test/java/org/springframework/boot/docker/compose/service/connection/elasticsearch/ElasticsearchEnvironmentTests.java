/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.elasticsearch;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ElasticsearchEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ElasticsearchEnvironmentTests {

	@Test
	void createWhenHasElasticPasswordFileThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new ElasticsearchEnvironment(Map.of("ELASTIC_PASSWORD_FILE", "afile")))
			.withMessage("ELASTIC_PASSWORD_FILE is not supported");
	}

	@Test
	void getPasswordWhenNoPassword() {
		ElasticsearchEnvironment environment = new ElasticsearchEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isNull();
	}

	@Test
	void getPasswordWhenHasPassword() {
		ElasticsearchEnvironment environment = new ElasticsearchEnvironment(Map.of("ELASTIC_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

}
