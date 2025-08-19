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

package org.springframework.boot.mongodb.autoconfigure;

import java.util.List;

import com.mongodb.ConnectionString;
import org.bson.UuidRepresentation;
import org.jspecify.annotations.Nullable;

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
 * @author Artsiom Yudovin
 * @author Safeer Ansari
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.mongodb")
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
	 * Protocol to be used for the MongoDB connection. Ignored if 'uri' is set.
	 */
	private String protocol = "mongodb";

	/**
	 * Mongo server host. Ignored if 'uri' is set.
	 */
	private @Nullable String host;

	/**
	 * Mongo server port. Ignored if 'uri' is set.
	 */
	private @Nullable Integer port;

	/**
	 * Additional server hosts. Ignored if 'uri' is set or if 'host' is omitted.
	 * Additional hosts will use the default mongo port of 27017. If you want to use a
	 * different port you can use the "host:port" syntax.
	 */
	private @Nullable List<String> additionalHosts;

	/**
	 * Mongo database URI. Overrides host, port, username, and password.
	 */
	private @Nullable String uri;

	/**
	 * Database name. Overrides database in URI.
	 */
	private @Nullable String database;

	/**
	 * Authentication database name.
	 */
	private @Nullable String authenticationDatabase;

	private final Gridfs gridfs = new Gridfs();

	/**
	 * Login user of the mongo server. Ignored if 'uri' is set.
	 */
	private @Nullable String username;

	/**
	 * Login password of the mongo server. Ignored if 'uri' is set.
	 */
	private char @Nullable [] password;

	/**
	 * Required replica set name for the cluster. Ignored if 'uri' is set.
	 */
	private @Nullable String replicaSetName;

	/**
	 * Fully qualified name of the FieldNamingStrategy to use.
	 */
	private @Nullable Class<?> fieldNamingStrategy;

	/**
	 * Representation to use when converting a UUID to a BSON binary value.
	 */
	private UuidRepresentation uuidRepresentation = UuidRepresentation.STANDARD;

	private final Ssl ssl = new Ssl();

	/**
	 * Whether to enable auto-index creation.
	 */
	private @Nullable Boolean autoIndexCreation;

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public @Nullable String getHost() {
		return this.host;
	}

	public void setHost(@Nullable String host) {
		this.host = host;
	}

	public @Nullable String getDatabase() {
		return this.database;
	}

	public void setDatabase(@Nullable String database) {
		this.database = database;
	}

	public @Nullable String getAuthenticationDatabase() {
		return this.authenticationDatabase;
	}

	public void setAuthenticationDatabase(@Nullable String authenticationDatabase) {
		this.authenticationDatabase = authenticationDatabase;
	}

	public @Nullable String getUsername() {
		return this.username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	public char @Nullable [] getPassword() {
		return this.password;
	}

	public void setPassword(char @Nullable [] password) {
		this.password = password;
	}

	public @Nullable String getReplicaSetName() {
		return this.replicaSetName;
	}

	public void setReplicaSetName(@Nullable String replicaSetName) {
		this.replicaSetName = replicaSetName;
	}

	public @Nullable Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	public void setFieldNamingStrategy(@Nullable Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public UuidRepresentation getUuidRepresentation() {
		return this.uuidRepresentation;
	}

	public void setUuidRepresentation(UuidRepresentation uuidRepresentation) {
		this.uuidRepresentation = uuidRepresentation;
	}

	public @Nullable String getUri() {
		return this.uri;
	}

	public String determineUri() {
		return (this.uri != null) ? this.uri : DEFAULT_URI;
	}

	public void setUri(@Nullable String uri) {
		this.uri = uri;
	}

	public @Nullable Integer getPort() {
		return this.port;
	}

	public void setPort(@Nullable Integer port) {
		this.port = port;
	}

	public Gridfs getGridfs() {
		return this.gridfs;
	}

	public @Nullable String getMongoClientDatabase() {
		if (this.database != null) {
			return this.database;
		}
		return new ConnectionString(determineUri()).getDatabase();
	}

	public @Nullable Boolean isAutoIndexCreation() {
		return this.autoIndexCreation;
	}

	public void setAutoIndexCreation(@Nullable Boolean autoIndexCreation) {
		this.autoIndexCreation = autoIndexCreation;
	}

	public @Nullable List<String> getAdditionalHosts() {
		return this.additionalHosts;
	}

	public void setAdditionalHosts(@Nullable List<String> additionalHosts) {
		this.additionalHosts = additionalHosts;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public static class Gridfs {

		/**
		 * GridFS database name.
		 */
		private @Nullable String database;

		/**
		 * GridFS bucket name.
		 */
		private @Nullable String bucket;

		public @Nullable String getDatabase() {
			return this.database;
		}

		public void setDatabase(@Nullable String database) {
			this.database = database;
		}

		public @Nullable String getBucket() {
			return this.bucket;
		}

		public void setBucket(@Nullable String bucket) {
			this.bucket = bucket;
		}

	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if "bundle" is provided
		 * unless specified otherwise.
		 */
		private @Nullable Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private @Nullable String bundle;

		public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

}
