/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.elasticsearch.docker.compose;

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
