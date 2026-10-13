/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.mongodb;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * A {@link MongoDBContainer} for MongoDB with SSL configuration.
 *
 * @author Scott Frederick
 */
class SecureMongoContainer extends MongoDBContainer {

	SecureMongoContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
	}

	@Override
	public void configure() {
		// test-server.pem is a single PEM file containing server certificate and key
		// content combined
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.pem"), "/ssl/server.pem");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-ca.crt"), "/ssl/ca.crt");
		withCommand("mongod --tlsMode requireTLS --tlsCertificateKeyFile /ssl/server.pem --tlsCAFile /ssl/ca.crt");
	}

	@Override
	protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
	}

}
