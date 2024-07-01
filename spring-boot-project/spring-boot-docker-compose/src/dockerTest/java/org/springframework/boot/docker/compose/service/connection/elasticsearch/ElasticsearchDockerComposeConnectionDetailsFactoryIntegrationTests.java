/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ElasticsearchDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ElasticsearchDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "elasticsearch-compose.yaml", image = TestImage.ELASTICSEARCH_8)
	void runCreatesConnectionDetails(ElasticsearchConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "elasticsearch-bitnami-compose.yaml", image = TestImage.BITNAMI_ELASTICSEARCH)
	void runWithBitnamiImageCreatesConnectionDetails(ElasticsearchConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	private void assertConnectionDetails(ElasticsearchConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("elastic");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getPathPrefix()).isNull();
		assertThat(connectionDetails.getNodes()).hasSize(1);
		Node node = connectionDetails.getNodes().get(0);
		assertThat(node.hostname()).isNotNull();
		assertThat(node.port()).isGreaterThan(0);
		assertThat(node.protocol()).isEqualTo(Protocol.HTTP);
		assertThat(node.username()).isEqualTo("elastic");
		assertThat(node.password()).isEqualTo("secret");
	}

}
