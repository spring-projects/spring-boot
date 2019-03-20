/*
 * Copyright 2012-2018 the original author or authors.
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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

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
 */
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoProperties {

	/**
	 * Default port used when the configured port is {@code null}.
	 */
	public static final int DEFAULT_PORT = 27017;

	public static final String DEFAULT_URI = "mongodb://localhost/test";

	/**
	 * Mongo server host. Cannot be set with uri.
	 */
	private String host;

	/**
	 * Mongo server port. Cannot be set with uri.
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
	 * Login user of the mongo server. Cannot be set with uri.
	 */
	private String username;

	/**
	 * Login password of the mongo server. Cannot be set with uri.
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

	public void clearPassword() {
		if (this.password == null) {
			return;
		}
		for (int i = 0; i < this.password.length; i++) {
			this.password[i] = 0;
		}
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

	/**
	 * Creates a {@link MongoClient} using the given {@code options} and
	 * {@code environment}. If the environment contains a {@code local.mongo.port}
	 * property, it is used to configure a client to an embedded MongoDB instance.
	 * @param options the options
	 * @param environment the environment
	 * @return the Mongo client
	 * @throws UnknownHostException if the configured host is unknown
	 */
	public MongoClient createMongoClient(MongoClientOptions options,
			Environment environment) throws UnknownHostException {
		try {
			Integer embeddedPort = getEmbeddedPort(environment);
			if (embeddedPort != null) {
				return createEmbeddedMongoClient(options, embeddedPort);
			}
			return createNetworkMongoClient(options);
		}
		finally {
			clearPassword();
		}
	}

	private Integer getEmbeddedPort(Environment environment) {
		if (environment != null) {
			String localPort = environment.getProperty("local.mongo.port");
			if (localPort != null) {
				return Integer.valueOf(localPort);
			}
		}
		return null;
	}

	private MongoClient createEmbeddedMongoClient(MongoClientOptions options, int port) {
		if (options == null) {
			options = MongoClientOptions.builder().build();
		}
		String host = (this.host != null) ? this.host : "localhost";
		return new MongoClient(Collections.singletonList(new ServerAddress(host, port)),
				Collections.<MongoCredential>emptyList(), options);
	}

	private MongoClient createNetworkMongoClient(MongoClientOptions options) {
		if (hasCustomAddress() || hasCustomCredentials()) {
			if (this.uri != null) {
				throw new IllegalStateException("Invalid mongo configuration, "
						+ "either uri or host/port/credentials must be specified");
			}
			if (options == null) {
				options = MongoClientOptions.builder().build();
			}
			List<MongoCredential> credentials = new ArrayList<MongoCredential>();
			if (hasCustomCredentials()) {
				String database = (this.authenticationDatabase != null)
						? this.authenticationDatabase : getMongoClientDatabase();
				credentials.add(MongoCredential.createCredential(this.username, database,
						this.password));
			}
			String host = (this.host != null) ? this.host : "localhost";
			int port = (this.port != null) ? this.port : DEFAULT_PORT;
			return new MongoClient(
					Collections.singletonList(new ServerAddress(host, port)), credentials,
					options);
		}
		// The options and credentials are in the URI
		return new MongoClient(new MongoClientURI(determineUri(), builder(options)));
	}

	private boolean hasCustomAddress() {
		return this.host != null || this.port != null;
	}

	private boolean hasCustomCredentials() {
		return this.username != null && this.password != null;
	}

	private Builder builder(MongoClientOptions options) {
		if (options != null) {
			return MongoClientOptions.builder(options);
		}
		return MongoClientOptions.builder();
	}

}
