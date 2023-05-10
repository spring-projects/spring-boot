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

import com.mongodb.ConnectionString;

/**
 * Adapts {@link MongoProperties} to {@link MongoConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class PropertiesMongoConnectionDetails implements MongoConnectionDetails {

	private final MongoProperties properties;

	public PropertiesMongoConnectionDetails(MongoProperties properties) {
		this.properties = properties;
	}

	@Override
	public ConnectionString getConnectionString() {
		// mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database.collection][?options]]
		if (this.properties.getUri() != null) {
			return new ConnectionString(this.properties.getUri());
		}
		StringBuilder builder = new StringBuilder("mongodb://");
		if (this.properties.getUsername() != null) {
			builder.append(this.properties.getUsername());
			builder.append(":");
			builder.append(this.properties.getPassword());
			builder.append("@");
		}
		builder.append((this.properties.getHost() != null) ? this.properties.getHost() : "localhost");
		if (this.properties.getPort() != null) {
			builder.append(":");
			builder.append(this.properties.getPort());
		}
		if (this.properties.getAdditionalHosts() != null) {
			builder.append(String.join(",", this.properties.getAdditionalHosts()));
		}
		if (this.properties.getMongoClientDatabase() != null || this.properties.getReplicaSetName() != null
				|| this.properties.getAuthenticationDatabase() != null) {
			builder.append("/");
			if (this.properties.getMongoClientDatabase() != null) {
				builder.append(this.properties.getMongoClientDatabase());
			}
			else if (this.properties.getAuthenticationDatabase() != null) {
				builder.append(this.properties.getAuthenticationDatabase());
			}
			if (this.properties.getReplicaSetName() != null) {
				builder.append("?");
				builder.append("replicaSet=");
				builder.append(this.properties.getReplicaSetName());
			}
		}
		return new ConnectionString(builder.toString());
	}

	@Override
	public GridFs getGridFs() {
		return GridFs.of(PropertiesMongoConnectionDetails.this.properties.getGridfs().getDatabase(),
				PropertiesMongoConnectionDetails.this.properties.getGridfs().getBucket());
	}

}
