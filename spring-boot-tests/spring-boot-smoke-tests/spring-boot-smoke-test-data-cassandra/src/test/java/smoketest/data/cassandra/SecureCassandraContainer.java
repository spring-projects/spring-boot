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

package smoketest.data.cassandra;

import org.testcontainers.utility.MountableFile;

import org.springframework.boot.testsupport.testcontainers.CassandraContainer;

/**
 * A {@link CassandraContainer} for Cassandra with SSL configuration.
 *
 * @author Scott Frederick
 */
class SecureCassandraContainer extends CassandraContainer {

	SecureCassandraContainer() {
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/cassandra.yaml"),
				"/etc/cassandra/cassandra.yaml");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.p12"),
				"/etc/cassandra/server.p12");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-ca.p12"),
				"/etc/cassandra/truststore.p12");
	}

}
