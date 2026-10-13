/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for an {@link OracleContainer#FREE} service.
 *
 * @author Andy Wilkinson
 */
class OracleFreeJdbcDockerComposeConnectionDetailsFactory extends OracleJdbcDockerComposeConnectionDetailsFactory {

	protected OracleFreeJdbcDockerComposeConnectionDetailsFactory() {
		super(OracleContainer.FREE);
	}

}
