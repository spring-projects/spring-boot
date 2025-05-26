/*
 * Copyright 2012-2025 the original author or authors.
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

package smoketest.kafka.ssl;

import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Kafka container with SSL enabled.
 *
 * @author Scott Frederick
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class SecureKafkaContainer extends ConfluentKafkaContainer {

	SecureKafkaContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
	}

	@Override
	protected void configure() {
		super.configure();
		withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SSL,BROKER:PLAINTEXT,CONTROLLER:PLAINTEXT")
			.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
			.withEnv("KAFKA_SSL_CLIENT_AUTH", "required")
			.withEnv("KAFKA_SSL_KEYSTORE_LOCATION", "/etc/kafka/secrets/certs/test-server.p12")
			.withEnv("KAFKA_SSL_KEYSTORE_PASSWORD", "password")
			.withEnv("KAFKA_SSL_KEY_PASSWORD", "password")
			.withEnv("KAFKA_SSL_TRUSTSTORE_LOCATION", "/etc/kafka/secrets/certs/test-ca.p12")
			.withEnv("KAFKA_SSL_TRUSTSTORE_PASSWORD", "password")
			.withEnv("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "");
		withCopyFileToContainer(MountableFile.forClasspathResource("ssl/test-server.p12"),
				"/etc/kafka/secrets/certs/test-server.p12");
		withCopyFileToContainer(MountableFile.forClasspathResource("ssl/credentials"),
				"/etc/kafka/secrets/certs/credentials");
		withCopyFileToContainer(MountableFile.forClasspathResource("ssl/test-ca.p12"),
				"/etc/kafka/secrets/certs/test-ca.p12");
	}

}
