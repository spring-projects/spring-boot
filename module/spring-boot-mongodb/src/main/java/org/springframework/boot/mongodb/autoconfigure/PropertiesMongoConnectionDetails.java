/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.autoconfigure;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.ConnectionString;
import org.jspecify.annotations.Nullable;

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
		String uri = this.properties.getUri();
		return (uri != null) ? new ConnectionString(uri) : buildConnectionString();
	}

	private ConnectionString buildConnectionString() {
		StringBuilder builder = new StringBuilder(getProtocol()).append("://");
		String username = this.properties.getUsername();
		char[] password = this.properties.getPassword();
		String host = this.properties.getHost();
		Integer port = this.properties.getPort();
		String database = this.properties.getMongoClientDatabase();
		List<String> additionalHosts = this.properties.getAdditionalHosts();
		if (username != null) {
			builder.append(encode(username));
			builder.append(":");
			builder.append((password != null) ? encode(new String(password)) : "");
			builder.append("@");
		}
		builder.append((host != null) ? host : "localhost");
		if (port != null) {
			builder.append(":");
			builder.append(port);
		}
		if (additionalHosts != null) {
			builder.append(",");
			builder.append(String.join(",", additionalHosts));
		}
		builder.append("/");
		builder.append((database != null) ? encode(database) : "");
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
		return URLEncoder.encode(input, StandardCharsets.UTF_8).replace("+", "%20");
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

	private List<String> getOptions() {
		String replicaSetName = this.properties.getReplicaSetName();
		String username = this.properties.getUsername();
		String authenticationDatabase = this.properties.getAuthenticationDatabase();
		List<String> options = new ArrayList<>();
		options.add(StringUtils.hasText(replicaSetName) ? "replicaSet=" + replicaSetName : "");
		if (username != null && authenticationDatabase != null) {
			options.add("authSource=" + authenticationDatabase);
		}
		return options;
	}

}
