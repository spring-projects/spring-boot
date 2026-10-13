/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.redis;

import com.redis.testcontainers.RedisContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * A {@link RedisContainer} for Redis with SSL configuration.
 *
 * @author Scott Frederick
 */
class SecureRedisContainer extends RedisContainer {

	SecureRedisContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.crt"), "/ssl/server.crt");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.key"), "/ssl/server.key");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-ca.crt"), "/ssl/ca.crt");
		withCommand("redis-server --tls-port 6379 --port 0 "
				+ "--tls-cert-file /ssl/server.crt --tls-key-file /ssl/server.key --tls-ca-cert-file /ssl/ca.crt");
	}

}
