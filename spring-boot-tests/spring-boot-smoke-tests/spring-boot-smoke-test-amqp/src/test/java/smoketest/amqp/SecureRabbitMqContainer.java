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

package smoketest.amqp;

import java.time.Duration;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

/**
 * A {@link RabbitMQContainer} for RabbitMQ with SSL configuration.
 *
 * @author Scott Frederick
 */
class SecureRabbitMqContainer extends RabbitMQContainer {

	SecureRabbitMqContainer() {
		super(DockerImageNames.rabbit());
		withStartupTimeout(Duration.ofMinutes(4));
	}

	@Override
	public void configure() {
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/rabbitmq.conf"),
				"/etc/rabbitmq/rabbitmq.conf");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.crt"),
				"/etc/rabbitmq/server_cert.pem");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.key"),
				"/etc/rabbitmq/server_key.pem");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-ca.crt"), "/etc/rabbitmq/ca_cert.pem");
	}

}
