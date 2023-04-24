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

package org.springframework.boot.docker.compose.service.connection;

import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ImageReference;
import org.springframework.boot.docker.compose.core.RunningService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionNamePredicate}.
 *
 * @author Phillip Webb
 */
class ConnectionNamePredicateTests {

	@Test
	void offical() {
		assertThat(predicateOf("elasticsearch")).accepts(sourceOf("elasticsearch"));
		assertThat(predicateOf("elasticsearch")).accepts(sourceOf("library/elasticsearch"));
		assertThat(predicateOf("elasticsearch")).accepts(sourceOf("docker.io/library/elasticsearch"));
		assertThat(predicateOf("elasticsearch")).accepts(sourceOf("docker.io/elasticsearch"));
		assertThat(predicateOf("elasticsearch")).accepts(sourceOf("docker.io/elasticsearch:latest"));
		assertThat(predicateOf("elasticsearch")).rejects(sourceOf("redis"));
		assertThat(predicateOf("elasticsearch")).rejects(sourceOf("library/redis"));
		assertThat(predicateOf("elasticsearch")).rejects(sourceOf("docker.io/library/redis"));
		assertThat(predicateOf("elasticsearch")).rejects(sourceOf("docker.io/redis"));
		assertThat(predicateOf("elasticsearch")).rejects(sourceOf("docker.io/redis"));
		assertThat(predicateOf("zipkin")).rejects(sourceOf("openzipkin/zipkin"));
	}

	@Test
	void organization() {
		assertThat(predicateOf("openzipkin/zipkin")).accepts(sourceOf("openzipkin/zipkin"));
		assertThat(predicateOf("openzipkin/zipkin")).accepts(sourceOf("openzipkin/zipkin:latest"));
		assertThat(predicateOf("openzipkin/zipkin")).rejects(sourceOf("openzipkin/zapkin"));
		assertThat(predicateOf("openzipkin/zipkin")).rejects(sourceOf("zipkin"));
	}

	@Test
	void customDomain() {
		assertThat(predicateOf("redis")).accepts(sourceOf("internalhost:8080/library/redis"));
		assertThat(predicateOf("redis")).accepts(sourceOf("myhost.com/library/redis"));
		assertThat(predicateOf("redis")).accepts(sourceOf("myhost.com:8080/library/redis"));
		assertThat(predicateOf("redis")).rejects(sourceOf("internalhost:8080/redis"));
	}

	@Test
	void labeled() {
		assertThat(predicateOf("redis")).accepts(sourceOf("internalhost:8080/myredis", "redis"));
		assertThat(predicateOf("redis")).accepts(sourceOf("internalhost:8080/myredis", "library/redis"));
		assertThat(predicateOf("openzipkin/zipkin"))
			.accepts(sourceOf("internalhost:8080/libs/libs/mzipkin", "openzipkin/zipkin"));
	}

	private Predicate<DockerComposeConnectionSource> predicateOf(String required) {
		return new ConnectionNamePredicate(required);
	}

	private DockerComposeConnectionSource sourceOf(String connectioName) {
		return sourceOf(connectioName, null);
	}

	private DockerComposeConnectionSource sourceOf(String connectioName, String label) {
		DockerComposeConnectionSource source = mock(DockerComposeConnectionSource.class);
		RunningService runningService = mock(RunningService.class);
		given(source.getRunningService()).willReturn(runningService);
		given(runningService.image()).willReturn(ImageReference.of(connectioName));
		if (label != null) {
			given(runningService.labels()).willReturn(Map.of("org.springframework.boot.service-connection", label));
		}
		return source;
	}

}
