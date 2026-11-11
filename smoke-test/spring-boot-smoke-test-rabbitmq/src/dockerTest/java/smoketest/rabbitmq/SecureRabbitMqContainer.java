/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.rabbitmq;

import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * A {@link RabbitMQContainer} for RabbitMQ with SSL configuration.
 *
 * @author Scott Frederick
 */
class SecureRabbitMqContainer extends RabbitMQContainer {

	SecureRabbitMqContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
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
