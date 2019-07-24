/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongo;

import com.mongodb.MongoClientURI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Mongo.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Josh Long
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Nasko Vasilev
 * @author Mark Paluch
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoProperties {

	/**
	 * Default port used when the configured port is {@code null}.
	 */
	public static final int DEFAULT_PORT = 27017;

	/**
	 * Default URI used when the configured URI is {@code null}.
	 */
	public static final String DEFAULT_URI = "mongodb://localhost/test";

	/**
	 * Mongo server host. Cannot be set with URI.
	 */
	private String host;

	/**
	 * Mongo server port. Cannot be set with URI.
	 */
	private Integer port = null;

	/**
	 * Mongo database URI. Cannot be set with host, port and credentials.
	 */
	private String uri;

	/**
	 * Database name.
	 */
	private String database;

	/**
	 * Authentication database name.
	 */
	private String authenticationDatabase;

	/**
	 * GridFS database name.
	 */
	private String gridFsDatabase;

	/**
	 * Login user of the mongo server. Cannot be set with URI.
	 */
	private String username;

	/**
	 * Login password of the mongo server. Cannot be set with URI.
	 */
	private char[] password;

	/**
	 * Fully qualified name of the FieldNamingStrategy to use.
	 */
	private Class<?> fieldNamingStrategy;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getDatabase() {
		return this.database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getAuthenticationDatabase() {
		return this.authenticationDatabase;
	}

	public void setAuthenticationDatabase(String authenticationDatabase) {
		this.authenticationDatabase = authenticationDatabase;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public char[] getPassword() {
		return this.password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	public void setFieldNamingStrategy(Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public String getUri() {
		return this.uri;
	}

	public String determineUri() {
		return (this.uri != null) ? this.uri : DEFAULT_URI;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getGridFsDatabase() {
		return this.gridFsDatabase;
	}

	public void setGridFsDatabase(String gridFsDatabase) {
		this.gridFsDatabase = gridFsDatabase;
	}

	public String getMongoClientDatabase() {
		if (this.database != null) {
			return this.database;
		}
		return new MongoClientURI(determineUri()).getDatabase();
	}

}
