/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import org.springframework.core.Ordered;

/**
 * A {@link MongoClientSettingsBuilderCustomizer} that applies properties from a
 * {@link MongoProperties} to a {@link MongoClientSettings}.
 *
 * @author Scott Frederick
 * @author Safeer Ansari
 * @since 2.4.0
 */
public class MongoPropertiesClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer, Ordered {

	private final MongoProperties properties;

	private static final Pattern ADDITIONAL_HOSTS_PATTERN = Pattern
			.compile("^\\[[0-9a-zA-Z]+(.)*(:)*(,[0-9a-zA-Z]+(.)*(:)*)*]$");

	private int order = 0;

	public MongoPropertiesClientSettingsBuilderCustomizer(MongoProperties properties) {
		this.properties = properties;
	}

	@Override
	public void customize(MongoClientSettings.Builder settingsBuilder) {
		applyUuidRepresentation(settingsBuilder);
		applyHostAndPort(settingsBuilder);
		applyCredentials(settingsBuilder);
		applyReplicaSet(settingsBuilder);
	}

	private void applyUuidRepresentation(MongoClientSettings.Builder settingsBuilder) {
		settingsBuilder.uuidRepresentation(this.properties.getUuidRepresentation());
	}

	private void applyHostAndPort(MongoClientSettings.Builder settings) {
		if (this.properties.getUri() != null) {
			settings.applyConnectionString(new ConnectionString(this.properties.getUri()));
			return;
		}
		if (this.properties.getHost() != null || this.properties.getPort() != null) {
			String host = getOrDefault(this.properties.getHost(), "localhost");
			int port = getOrDefault(this.properties.getPort(), MongoProperties.DEFAULT_PORT);
			ServerAddress serverAddress = new ServerAddress(host, port);
			List<ServerAddress> serverAddressList = new ArrayList<>(List.of(serverAddress));
			applyAdditionalHosts(serverAddressList);
			settings.applyToClusterSettings((cluster) -> cluster.hosts(serverAddressList));
			return;
		}
		settings.applyConnectionString(new ConnectionString(MongoProperties.DEFAULT_URI));
	}

	private void applyAdditionalHosts(List<ServerAddress> serverAddressList) {
		if (this.properties.getAdditionalHosts() != null && !this.properties.getAdditionalHosts().isEmpty()) {
			Matcher matcher = ADDITIONAL_HOSTS_PATTERN.matcher((this.properties.getAdditionalHosts()));
			if (!matcher.matches()) {
				return;
			}

			String[] additionalHosts = this.properties.getAdditionalHosts()
					.substring(1, this.properties.getAdditionalHosts().length() - 1).split(",");
			Arrays.stream(additionalHosts)
					.forEach((additionalHost) -> serverAddressList.add(new ServerAddress(additionalHost)));
		}
	}

	private void applyCredentials(MongoClientSettings.Builder builder) {
		if (this.properties.getUri() == null && this.properties.getUsername() != null
				&& this.properties.getPassword() != null) {
			String database = (this.properties.getAuthenticationDatabase() != null)
					? this.properties.getAuthenticationDatabase() : this.properties.getMongoClientDatabase();
			builder.credential((MongoCredential.createCredential(this.properties.getUsername(), database,
					this.properties.getPassword())));
		}
	}

	private void applyReplicaSet(MongoClientSettings.Builder builder) {
		if (this.properties.getReplicaSetName() != null) {
			builder.applyToClusterSettings(
					(cluster) -> cluster.requiredReplicaSetName(this.properties.getReplicaSetName()));
		}
	}

	private <V> V getOrDefault(V value, V defaultValue) {
		return (value != null) ? value : defaultValue;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order value of this object.
	 * @param order the new order value
	 * @see #getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
