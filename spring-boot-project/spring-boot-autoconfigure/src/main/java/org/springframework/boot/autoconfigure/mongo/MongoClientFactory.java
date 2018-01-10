/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import org.springframework.core.env.Environment;

/**
 * A factory for a blocking {@link MongoClient} that applies {@link MongoProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Josh Long
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Nasko Vasilev
 * @author Mark Paluch
 * @since 2.0.0
 */
public class MongoClientFactory {

	private final MongoProperties properties;

	private final Environment environment;

	public MongoClientFactory(MongoProperties properties, Environment environment) {
		this.properties = properties;
		this.environment = environment;
	}

	/**
	 * Creates a {@link MongoClient} using the given {@code options}. If the environment
	 * contains a {@code local.mongo.port} property, it is used to configure a client to
	 * an embedded MongoDB instance.
	 * @param options the options
	 * @return the Mongo client
	 */
	public MongoClient createMongoClient(MongoClientOptions options) {
		Integer embeddedPort = getEmbeddedPort();
		if (embeddedPort != null) {
			return createEmbeddedMongoClient(options, embeddedPort);
		}
		return createNetworkMongoClient(options);
	}

	private Integer getEmbeddedPort() {
		if (this.environment != null) {
			String localPort = this.environment.getProperty("local.mongo.port");
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
		String host = this.properties.getHost() == null ? "localhost"
				: this.properties.getHost();
		return new MongoClient(Collections.singletonList(new ServerAddress(host, port)),
				options);
	}

	private MongoClient createNetworkMongoClient(MongoClientOptions options) {
		MongoProperties properties = this.properties;
		if (properties.getUri() != null) {
			return createMongoClient(properties.getUri(), options);
		}
		if (hasCustomAddress() || hasCustomCredentials()) {
			if (options == null) {
				options = MongoClientOptions.builder().build();
			}
			MongoCredential credentials = getCredentials(properties);
			String host = getValue(properties.getHost(), "localhost");
			int port = getValue(properties.getPort(), MongoProperties.DEFAULT_PORT);
			List<ServerAddress> seeds = Collections
					.singletonList(new ServerAddress(host, port));
			return credentials == null ? new MongoClient(seeds, options)
					: new MongoClient(seeds, credentials, options);
		}
		return createMongoClient(MongoProperties.DEFAULT_URI, options);
	}

	private MongoClient createMongoClient(String uri, MongoClientOptions options) {
		return new MongoClient(new MongoClientURI(uri, builder(options)));
	}

	private <T> T getValue(T value, T fallback) {
		return (value == null ? fallback : value);
	}

	private boolean hasCustomAddress() {
		return this.properties.getHost() != null || this.properties.getPort() != null;
	}

	private MongoCredential getCredentials(MongoProperties properties) {
		if (!hasCustomCredentials()) {
			return null;
		}
		String username = properties.getUsername();
		String database = getValue(properties.getAuthenticationDatabase(),
				properties.getMongoClientDatabase());
		char[] password = properties.getPassword();
		return MongoCredential.createCredential(username, database, password);
	}

	private boolean hasCustomCredentials() {
		return this.properties.getUsername() != null
				&& this.properties.getPassword() != null;
	}

	private Builder builder(MongoClientOptions options) {
		if (options != null) {
			return MongoClientOptions.builder(options);
		}
		return MongoClientOptions.builder();
	}

}
