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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.ConnectionString;

/**
 * Adapts {@link MongoProperties} to {@link MongoConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
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
			builder.append(encode(this.properties.getUsername()));
			builder.append(":");
			if (this.properties.getPassword() != null) {
				builder.append(encode(this.properties.getPassword()));
			}
			builder.append("@");
		}
		builder.append((this.properties.getHost() != null) ? this.properties.getHost() : "localhost");
		if (this.properties.getPort() != null) {
			builder.append(":");
			builder.append(this.properties.getPort());
		}
		if (this.properties.getAdditionalHosts() != null) {
			builder.append(",");
			builder.append(String.join(",", this.properties.getAdditionalHosts()));
		}
		builder.append("/");
		builder.append(this.properties.getMongoClientDatabase());
		List<String> options = getOptions();
		if (!options.isEmpty()) {
			builder.append("?");
			builder.append(String.join("&", options));
		}
		return new ConnectionString(builder.toString());
	}

	private String encode(String input) {
		return URLEncoder.encode(input, StandardCharsets.UTF_8);
	}

	private char[] encode(char[] input) {
		return URLEncoder.encode(new String(input), StandardCharsets.UTF_8).toCharArray();
	}

	@Override
	public GridFs getGridFs() {
		return GridFs.of(PropertiesMongoConnectionDetails.this.properties.getGridfs().getDatabase(),
				PropertiesMongoConnectionDetails.this.properties.getGridfs().getBucket());
	}

	private List<String> getOptions() {
		List<String> options = new ArrayList<>();
		if (this.properties.getReplicaSetName() != null) {
			options.add("replicaSet=" + this.properties.getReplicaSetName());
		}
		if (this.properties.getUsername() != null && this.properties.getAuthenticationDatabase() != null) {
			options.add("authSource=" + this.properties.getAuthenticationDatabase());
		}
		return options;
	}

}
