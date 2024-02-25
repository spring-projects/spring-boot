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

package org.springframework.boot.autoconfigure.mongo;

import java.util.List;

import com.mongodb.ConnectionString;
import org.bson.UuidRepresentation;

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
	 * Additional server hosts. Cannot be set with URI or if 'host' is not specified.
	 * Additional hosts will use the default mongo port of 27017. If you want to use a
	 * different port you can use the "host:port" syntax.
	 */
	private List<String> additionalHosts;

	/**
	 * Mongo database URI. Overrides host, port, username, and password.
	 */
	private String uri;

	/**
	 * Database name. Overrides database in URI.
	 */
	private String database;

	/**
	 * Authentication database name.
	 */
	private String authenticationDatabase;

	private final Gridfs gridfs = new Gridfs();

	/**
	 * Login user of the mongo server. Cannot be set with URI.
	 */
	private String username;

	/**
	 * Login password of the mongo server. Cannot be set with URI.
	 */
	private char[] password;

	/**
	 * Required replica set name for the cluster. Cannot be set with URI.
	 */
	private String replicaSetName;

	/**
	 * Fully qualified name of the FieldNamingStrategy to use.
	 */
	private Class<?> fieldNamingStrategy;

	/**
	 * Representation to use when converting a UUID to a BSON binary value.
	 */
	private UuidRepresentation uuidRepresentation = UuidRepresentation.JAVA_LEGACY;

	private final Ssl ssl = new Ssl();

	/**
	 * Whether to enable auto-index creation.
	 */
	private Boolean autoIndexCreation;

	/**
	 * Returns the host of the MongoProperties.
	 * @return the host of the MongoProperties
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Sets the host for the MongoDB connection.
	 * @param host the host address or hostname to connect to
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Returns the name of the database.
	 * @return the name of the database
	 */
	public String getDatabase() {
		return this.database;
	}

	/**
	 * Sets the name of the database to be used.
	 * @param database the name of the database
	 */
	public void setDatabase(String database) {
		this.database = database;
	}

	/**
	 * Returns the authentication database used for authentication.
	 * @return the authentication database
	 */
	public String getAuthenticationDatabase() {
		return this.authenticationDatabase;
	}

	/**
	 * Sets the authentication database for the MongoDB connection.
	 * @param authenticationDatabase the authentication database to be set
	 */
	public void setAuthenticationDatabase(String authenticationDatabase) {
		this.authenticationDatabase = authenticationDatabase;
	}

	/**
	 * Returns the username associated with the MongoProperties object.
	 * @return the username
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the username for the MongoProperties.
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Returns the password as a character array.
	 * @return the password as a character array
	 */
	public char[] getPassword() {
		return this.password;
	}

	/**
	 * Sets the password for the MongoProperties.
	 * @param password the password to be set
	 */
	public void setPassword(char[] password) {
		this.password = password;
	}

	/**
	 * Returns the name of the replica set.
	 * @return the name of the replica set
	 */
	public String getReplicaSetName() {
		return this.replicaSetName;
	}

	/**
	 * Sets the name of the replica set.
	 * @param replicaSetName the name of the replica set
	 */
	public void setReplicaSetName(String replicaSetName) {
		this.replicaSetName = replicaSetName;
	}

	/**
	 * Returns the field naming strategy used by this MongoProperties instance.
	 * @return the field naming strategy used by this MongoProperties instance
	 */
	public Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	/**
	 * Sets the field naming strategy for the MongoProperties class.
	 * @param fieldNamingStrategy the field naming strategy to be set
	 */
	public void setFieldNamingStrategy(Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	/**
	 * Returns the UUID representation used by this MongoProperties instance.
	 * @return the UUID representation
	 */
	public UuidRepresentation getUuidRepresentation() {
		return this.uuidRepresentation;
	}

	/**
	 * Sets the UUID representation for this MongoProperties object.
	 * @param uuidRepresentation the UUID representation to be set
	 */
	public void setUuidRepresentation(UuidRepresentation uuidRepresentation) {
		this.uuidRepresentation = uuidRepresentation;
	}

	/**
	 * Returns the URI of the MongoProperties.
	 * @return the URI of the MongoProperties
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * Determines the URI for the MongoProperties object. If the uri is not null, returns
	 * the uri. Otherwise, returns the default URI.
	 * @return the determined URI
	 */
	public String determineUri() {
		return (this.uri != null) ? this.uri : DEFAULT_URI;
	}

	/**
	 * Sets the URI for the MongoProperties.
	 * @param uri the URI to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Returns the port number.
	 * @return the port number
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the port for the MongoDB connection.
	 * @param port the port number to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * Returns the GridFS instance associated with this MongoProperties object.
	 * @return the GridFS instance
	 */
	public Gridfs getGridfs() {
		return this.gridfs;
	}

	/**
	 * Returns the name of the MongoDB database to be used by the MongoClient. If the
	 * database name is already set, it will be returned. Otherwise, a new
	 * ConnectionString object will be created using the determined URI, and the database
	 * name from the ConnectionString will be returned.
	 * @return the name of the MongoDB database
	 */
	public String getMongoClientDatabase() {
		if (this.database != null) {
			return this.database;
		}
		return new ConnectionString(determineUri()).getDatabase();
	}

	/**
	 * Returns the value indicating whether auto index creation is enabled.
	 * @return true if auto index creation is enabled, false otherwise
	 */
	public Boolean isAutoIndexCreation() {
		return this.autoIndexCreation;
	}

	/**
	 * Sets the flag for auto index creation.
	 * @param autoIndexCreation the flag indicating whether auto index creation is enabled
	 * or not
	 */
	public void setAutoIndexCreation(Boolean autoIndexCreation) {
		this.autoIndexCreation = autoIndexCreation;
	}

	/**
	 * Returns the additional hosts for the MongoProperties.
	 * @return the additional hosts for the MongoProperties
	 */
	public List<String> getAdditionalHosts() {
		return this.additionalHosts;
	}

	/**
	 * Sets the additional hosts for the MongoProperties.
	 * @param additionalHosts the list of additional hosts to be set
	 */
	public void setAdditionalHosts(List<String> additionalHosts) {
		this.additionalHosts = additionalHosts;
	}

	/**
	 * Returns the SSL configuration for the MongoProperties.
	 * @return the SSL configuration for the MongoProperties
	 */
	public Ssl getSsl() {
		return this.ssl;
	}

	/**
	 * Gridfs class.
	 */
	public static class Gridfs {

		/**
		 * GridFS database name.
		 */
		private String database;

		/**
		 * GridFS bucket name.
		 */
		private String bucket;

		/**
		 * Returns the name of the database associated with this Gridfs instance.
		 * @return the name of the database
		 */
		public String getDatabase() {
			return this.database;
		}

		/**
		 * Sets the database for the Gridfs class.
		 * @param database the name of the database to be set
		 */
		public void setDatabase(String database) {
			this.database = database;
		}

		/**
		 * Returns the name of the bucket associated with this Gridfs instance.
		 * @return the name of the bucket
		 */
		public String getBucket() {
			return this.bucket;
		}

		/**
		 * Sets the bucket for the Gridfs class.
		 * @param bucket the name of the bucket to be set
		 */
		public void setBucket(String bucket) {
			this.bucket = bucket;
		}

	}

	/**
	 * Ssl class.
	 */
	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if "bundle" is provided
		 * unless specified otherwise.
		 */
		private Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private String bundle;

		/**
		 * Returns a boolean value indicating whether the SSL is enabled.
		 * @return true if SSL is enabled, false otherwise
		 */
		public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		/**
		 * Sets the enabled status of the Ssl object.
		 * @param enabled the boolean value indicating whether the Ssl object is enabled
		 * or not
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Returns the bundle associated with this Ssl object.
		 * @return the bundle associated with this Ssl object
		 */
		public String getBundle() {
			return this.bundle;
		}

		/**
		 * Sets the bundle for the Ssl class.
		 * @param bundle the bundle to be set
		 */
		public void setBundle(String bundle) {
			this.bundle = bundle;
		}

	}

}
