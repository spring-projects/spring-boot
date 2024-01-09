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

package org.springframework.boot.testcontainers.service.connection.elasticsearch;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class Elasticsearch7ContainerConnectionDetailsFactoryIntegrationTest {

	@Container
	@ServiceConnection
	static final ElasticsearchContainer esContainer =
			new ElasticsearchContainer(DockerImageNames.elasticsearch())
					.withEnv("xpack.security.enabled", "true")
					.withPassword("correct horse battery staple");

	@Autowired
	private ElasticsearchConnectionDetails connectionDetails;

	@Test
	void connectionDetailsShouldBeSet() {
		assertThat(this.connectionDetails).isNotNull();
		assertThat(this.connectionDetails.getPassword()).isEqualTo("correct horse battery staple");
		assertThat(this.connectionDetails.getUsername()).isEqualTo("elastic");
		List<Node> nodes = this.connectionDetails.getNodes();
		assertThat(nodes).hasSize(1);
		assertThat(nodes.get(0).protocol()).isEqualTo(Protocol.HTTP);
		assertThat(nodes.get(0).port()).isEqualTo(esContainer.getMappedPort(9200));
	}
}