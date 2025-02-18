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

package org.springframework.boot.docker.compose.service.connection.oracle;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link R2dbcConnectionDetails}
 * for an {@link OracleContainer#FREE} service.
 *
 * @author Andy Wilkinson
 */
class OracleFreeR2dbcDockerComposeConnectionDetailsFactory extends OracleR2dbcDockerComposeConnectionDetailsFactory {

	protected OracleFreeR2dbcDockerComposeConnectionDetailsFactory() {
		super(OracleContainer.FREE);
	}

}
