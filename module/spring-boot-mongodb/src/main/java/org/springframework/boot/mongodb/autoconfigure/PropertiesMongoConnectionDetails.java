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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.mongodb.ConnectionString;
import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.Tag;
import com.mongodb.TagSet;
import com.mongodb.WriteConcern;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link MongoProperties} to {@link MongoConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jay Choi
 * @since 4.0.0
 */
public class PropertiesMongoConnectionDetails implements MongoConnectionDetails {

	private final MongoProperties properties;

	private final @Nullable SslBundles sslBundles;

	public PropertiesMongoConnectionDetails(MongoProperties properties, @Nullable SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public ConnectionString getConnectionString() {
		// protocol://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database.collection][?options]]
		if (this.properties.getUri() != null) {
			return new ConnectionString(this.properties.getUri());
		}
		StringBuilder builder = new StringBuilder(getProtocol()).append("://");
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

	private String getProtocol() {
		String protocol = this.properties.getProtocol();
		if (StringUtils.hasText(protocol)) {
			return protocol;
		}
		return "mongodb";
	}

	private String encode(String input) {
		return URLEncoder.encode(input, StandardCharsets.UTF_8);
	}

	private char[] encode(char[] input) {
		return URLEncoder.encode(new String(input), StandardCharsets.UTF_8).toCharArray();
	}

	@Override
	public @Nullable SslBundle getSslBundle() {
		Ssl ssl = this.properties.getSsl();
		if (!ssl.isEnabled()) {
			return null;
		}
		if (StringUtils.hasLength(ssl.getBundle())) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(ssl.getBundle());
		}
		return SslBundle.systemDefault();
	}

	@Override
	public @Nullable ReadConcern getReadConcern() {
		String readConcernLevel = this.properties.getReadConcern();
		if (readConcernLevel == null) {
			return null;
		}
		return new ReadConcern(ReadConcernLevel.fromString(readConcernLevel));
	}

	@Override
	public @Nullable WriteConcern getWriteConcern() {
		String w = this.properties.getWriteConcern().getW();
		Duration wTimeout = this.properties.getWriteConcern().getWTimeout();
		Boolean journal = this.properties.getWriteConcern().getJournal();
		WriteConcern writeConcern = null;
		if (w != null || wTimeout != null || journal != null) {
			if (w == null) {
				writeConcern = WriteConcern.ACKNOWLEDGED;
			}
			else {
				try {
					writeConcern = new WriteConcern(Integer.parseInt(w));
				}
				catch (NumberFormatException ex) {
					writeConcern = new WriteConcern(w);
				}
			}
			if (wTimeout != null) {
				writeConcern = writeConcern.withWTimeout(wTimeout.toMillis(), TimeUnit.MILLISECONDS);
			}
			if (journal != null) {
				writeConcern = writeConcern.withJournal(journal);
			}
		}
		return writeConcern;
	}

	@Override
	public @Nullable ReadPreference getReadPreference() {
		String mode = this.properties.getReadPreference().getMode();
		List<Map<String, String>> tags = this.properties.getReadPreference().getTags();
		Duration maxStaleness = this.properties.getReadPreference().getMaxStaleness();
		List<TagSet> tagSetList = getTagSets(tags);
		if (mode != null) {
			if (tagSetList.isEmpty() && maxStaleness == null) {
				return ReadPreference.valueOf(mode);
			}
			else if (maxStaleness == null) {
				return ReadPreference.valueOf(mode, tagSetList);
			}
			else {
				return ReadPreference.valueOf(mode, tagSetList, maxStaleness.toSeconds(), TimeUnit.SECONDS);
			}
		}
		else if (!tagSetList.isEmpty() || maxStaleness != null) {
			throw new InvalidConfigurationPropertyValueException("spring.mongodb.read-preference.mode", null,
					"Read preference mode must be specified if tags or max-staleness is specified");
		}
		return null;
	}

	private List<String> getOptions() {
		List<String> options = new ArrayList<>();
		if (StringUtils.hasText(this.properties.getReplicaSetName())) {
			options.add("replicaSet=" + this.properties.getReplicaSetName());
		}
		if (this.properties.getUsername() != null && this.properties.getAuthenticationDatabase() != null) {
			options.add("authSource=" + this.properties.getAuthenticationDatabase());
		}
		return options;
	}

	private List<TagSet> getTagSets(@Nullable List<Map<String, String>> tags) {
		List<TagSet> tagSetList = new ArrayList<>();
		if (tags != null && !tags.isEmpty()) {
			for (Map<String, String> tag : tags) {
				if (tag == null || tag.isEmpty()) {
					tagSetList.add(new TagSet());
				}
				else {
					List<Tag> tagList = new ArrayList<>();
					tag.forEach((key, value) -> tagList.add(new Tag(key, value)));
					tagSetList.add(new TagSet(tagList));
				}
			}
		}
		return tagSetList;
	}

}
