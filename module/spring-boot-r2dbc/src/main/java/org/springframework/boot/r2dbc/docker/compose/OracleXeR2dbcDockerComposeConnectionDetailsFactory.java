/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.docker.compose;

import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link R2dbcConnectionDetails}
 * for an {@link OracleContainer#XE} service.
 *
 * @author Andy Wilkinson
 */
class OracleXeR2dbcDockerComposeConnectionDetailsFactory extends OracleR2dbcDockerComposeConnectionDetailsFactory {

	protected OracleXeR2dbcDockerComposeConnectionDetailsFactory() {
		super(OracleContainer.XE);
	}

}
