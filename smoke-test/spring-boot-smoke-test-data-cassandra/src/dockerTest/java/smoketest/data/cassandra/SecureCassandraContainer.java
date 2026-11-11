/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.cassandra;

import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * A {@link CassandraContainer} for Cassandra with SSL configuration.
 *
 * @author Scott Frederick
 */
class SecureCassandraContainer extends CassandraContainer {

	SecureCassandraContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		setWaitStrategy(Wait.defaultWaitStrategy()); // default strategy uses plain text
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/cassandra.yaml"),
				"/etc/cassandra/cassandra.yaml");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.p12"),
				"/etc/cassandra/server.p12");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-ca.p12"),
				"/etc/cassandra/truststore.p12");
	}

}
