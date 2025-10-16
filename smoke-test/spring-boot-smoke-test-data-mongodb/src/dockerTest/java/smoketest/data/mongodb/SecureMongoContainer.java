/*
 * Copyright 2012-present the original author or authors.
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
