/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mongo;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * Configuration properties for Mongo.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Josh Long
 */
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoProperties {

	private String host;

	private int port = 27017;

	private String uri = "mongodb://localhost/test";

	private String database;

	private String authenticationDatabase;

	private String gridFsDatabase;

	private String username;

	private char[] password;

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

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
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
		return new MongoClientURI(this.uri).getDatabase();
	}

	public MongoClient createMongoClient(MongoClientOptions options)
			throws UnknownHostException {
		try {
			if (this.host != null) {
				if (options == null) {
					options = MongoClientOptions.builder().build();
				}
				List<MongoCredential> credentials = null;
				if (this.password != null && this.username != null) {
					credentials = Arrays.asList(MongoCredential.createMongoCRCredential(
							this.username, getMongoClientDatabase(), this.password));
				}
				return new MongoClient(Arrays.asList(new ServerAddress(this.host,
						this.port)), credentials, options);
			}
			// The options and credentials are in the URI
			return new MongoClient(new MongoClientURI(this.uri, builder(options)));
		}
		finally {
			clearPassword();
		}
	}

	private Builder builder(MongoClientOptions options) {
		Builder builder = MongoClientOptions.builder();
		if (options != null) {
			builder.alwaysUseMBeans(options.isAlwaysUseMBeans());
			builder.connectionsPerHost(options.getConnectionsPerHost());
			builder.connectTimeout(options.getConnectTimeout());
			builder.cursorFinalizerEnabled(options.isCursorFinalizerEnabled());
			builder.dbDecoderFactory(options.getDbDecoderFactory());
			builder.dbEncoderFactory(options.getDbEncoderFactory());
			builder.description(options.getDescription());
			builder.maxWaitTime(options.getMaxWaitTime());
			builder.readPreference(options.getReadPreference());
			builder.socketFactory(options.getSocketFactory());
			builder.socketKeepAlive(options.isSocketKeepAlive());
			builder.socketTimeout(options.getSocketTimeout());
			builder.threadsAllowedToBlockForConnectionMultiplier(options
					.getThreadsAllowedToBlockForConnectionMultiplier());
			builder.writeConcern(options.getWriteConcern());
		}
		return builder;
	}

}
