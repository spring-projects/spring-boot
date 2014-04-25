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

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.mongodb.DBPort;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

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

	private int port = DBPort.PORT;

	private String uri = "mongodb://localhost/test";

	private String database;

	private String gridFsDatabase;

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

	public MongoClient createMongoClient() throws UnknownHostException {
		if (this.host != null) {
			return new MongoClient(this.host, this.port);
		}
		return new MongoClient(new MongoClientURI(this.uri));
	}

}
