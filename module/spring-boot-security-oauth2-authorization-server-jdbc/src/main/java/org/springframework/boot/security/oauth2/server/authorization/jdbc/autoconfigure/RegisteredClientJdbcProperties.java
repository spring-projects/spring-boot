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

package org.springframework.boot.security.oauth2.server.authorization.jdbc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.init.DatabaseInitializationProperties;

/**
 * Configuration properties for the JDBC-backed registered client repository.
 *
 * @author Chaitanya Pawar
 * @since 4.2.0
 */
@ConfigurationProperties("spring.security.oauth2.authorizationserver.client.jdbc")
public class RegisteredClientJdbcProperties extends DatabaseInitializationProperties {

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/security/oauth2/server/"
			+ "authorization/client/oauth2-registered-client-schema.sql";

	@Override
	public String getDefaultSchemaLocation() {
		return DEFAULT_SCHEMA_LOCATION;
	}

}
