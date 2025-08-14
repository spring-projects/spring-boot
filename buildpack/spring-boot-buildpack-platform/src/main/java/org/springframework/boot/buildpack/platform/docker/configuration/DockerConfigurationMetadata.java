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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.boot.buildpack.platform.system.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Docker configuration stored in metadata files managed by the Docker CLI.
 *
 * @author Scott Frederick
 * @author Dmytro Nosan
 */
final class DockerConfigurationMetadata {

	private static final String DOCKER_CONFIG = "DOCKER_CONFIG";

	private static final String DEFAULT_CONTEXT = "default";

	private static final String CONFIG_DIR = ".docker";

	private static final String CONTEXTS_DIR = "contexts";

	private static final String META_DIR = "meta";

	private static final String TLS_DIR = "tls";

	private static final String DOCKER_ENDPOINT = "docker";

	private static final String CONFIG_FILE_NAME = "config.json";

	private static final String CONTEXT_FILE_NAME = "meta.json";

	private static final Supplier<DockerConfigurationMetadata> systemEnvironmentConfigurationMetadata = SingletonSupplier
		.of(() -> DockerConfigurationMetadata.create(Environment.SYSTEM));

	private final String configLocation;

	private final DockerConfig config;

	private final DockerContext context;

	private DockerConfigurationMetadata(String configLocation, DockerConfig config, DockerContext context) {
		this.configLocation = configLocation;
		this.config = config;
		this.context = context;
	}

	DockerConfig getConfiguration() {
		return this.config;
	}

	DockerContext getContext() {
		return this.context;
	}

	DockerContext forContext(@Nullable String context) {
		return createDockerContext(this.configLocation, context);
	}

	static DockerConfigurationMetadata from(Environment environment) {
		if (environment == Environment.SYSTEM) {
			return systemEnvironmentConfigurationMetadata.get();
		}
		return create(environment);
	}

	private static DockerConfigurationMetadata create(Environment environment) {
		String configLocation = environment.get(DOCKER_CONFIG);
		configLocation = (configLocation != null) ? configLocation : getUserHomeConfigLocation();
		DockerConfig dockerConfig = createDockerConfig(configLocation);
		DockerContext dockerContext = createDockerContext(configLocation, dockerConfig.getCurrentContext());
		return new DockerConfigurationMetadata(configLocation, dockerConfig, dockerContext);
	}

	private static String getUserHomeConfigLocation() {
		return Path.of(System.getProperty("user.home"), CONFIG_DIR).toString();
	}

	private static DockerConfig createDockerConfig(String configLocation) {
		Path path = Path.of(configLocation, CONFIG_FILE_NAME);
		if (!path.toFile().exists()) {
			return DockerConfig.empty();
		}
		try {
			return DockerConfig.fromJson(readPathContent(path));
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Error parsing Docker configuration file '" + path + "'", ex);
		}
	}

	private static DockerContext createDockerContext(String configLocation, @Nullable String currentContext) {
		if (currentContext == null || DEFAULT_CONTEXT.equals(currentContext)) {
			return DockerContext.empty();
		}
		String hash = asHash(currentContext);
		Path metaPath = Path.of(configLocation, CONTEXTS_DIR, META_DIR, hash, CONTEXT_FILE_NAME);
		Path tlsPath = Path.of(configLocation, CONTEXTS_DIR, TLS_DIR, hash, DOCKER_ENDPOINT);
		if (!metaPath.toFile().exists()) {
			throw new IllegalArgumentException("Docker context '" + currentContext + "' does not exist");
		}
		try {
			DockerContext context = DockerContext.fromJson(readPathContent(metaPath));
			if (tlsPath.toFile().isDirectory()) {
				return context.withTlsPath(tlsPath.toString());
			}
			return context;
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Error parsing Docker context metadata file '" + metaPath + "'", ex);
		}
	}

	private static String asHash(String currentContext) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(currentContext.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	private static String readPathContent(Path path) {
		try {
			return Files.readString(path);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Error reading Docker configuration file '" + path + "'", ex);
		}
	}

	static final class DockerConfig extends MappedObject {

		private final @Nullable String currentContext;

		private final @Nullable String credsStore;

		private final Map<String, String> credHelpers;

		private final Map<String, Auth> auths;

		private DockerConfig(JsonNode node) {
			super(node, MethodHandles.lookup());
			this.currentContext = valueAt("/currentContext", String.class);
			this.credsStore = valueAt("/credsStore", String.class);
			this.credHelpers = mapAt("/credHelpers", JsonNode::textValue);
			this.auths = mapAt("/auths", Auth::new);
		}

		@Nullable String getCurrentContext() {
			return this.currentContext;
		}

		@Nullable String getCredsStore() {
			return this.credsStore;
		}

		Map<String, String> getCredHelpers() {
			return this.credHelpers;
		}

		Map<String, Auth> getAuths() {
			return this.auths;
		}

		static DockerConfig fromJson(String json) throws JsonProcessingException {
			return new DockerConfig(SharedObjectMapper.get().readTree(json));
		}

		static DockerConfig empty() {
			return new DockerConfig(NullNode.instance);
		}

	}

	static final class Auth extends MappedObject {

		private final @Nullable String username;

		private final @Nullable String password;

		private final @Nullable String email;

		Auth(JsonNode node) {
			super(node, MethodHandles.lookup());
			String auth = valueAt("/auth", String.class);
			if (StringUtils.hasLength(auth)) {
				String[] parts = new String(Base64.getDecoder().decode(auth)).split(":", 2);
				Assert.state(parts.length == 2, "Malformed auth in docker configuration metadata");
				this.username = parts[0];
				this.password = trim(parts[1], Character.MIN_VALUE);
			}
			else {
				this.username = valueAt("/username", String.class);
				this.password = valueAt("/password", String.class);
			}
			this.email = valueAt("/email", String.class);
		}

		@Nullable String getUsername() {
			return this.username;
		}

		@Nullable String getPassword() {
			return this.password;
		}

		@Nullable String getEmail() {
			return this.email;
		}

		private static String trim(String source, char character) {
			source = StringUtils.trimLeadingCharacter(source, character);
			return StringUtils.trimTrailingCharacter(source, character);
		}

	}

	static final class DockerContext extends MappedObject {

		private final @Nullable String dockerHost;

		private final @Nullable Boolean skipTlsVerify;

		private final @Nullable String tlsPath;

		private DockerContext(JsonNode node, @Nullable String tlsPath) {
			super(node, MethodHandles.lookup());
			this.dockerHost = valueAt("/Endpoints/" + DOCKER_ENDPOINT + "/Host", String.class);
			this.skipTlsVerify = valueAt("/Endpoints/" + DOCKER_ENDPOINT + "/SkipTLSVerify", Boolean.class);
			this.tlsPath = tlsPath;
		}

		@Nullable String getDockerHost() {
			return this.dockerHost;
		}

		Boolean isTlsVerify() {
			return this.skipTlsVerify != null && !this.skipTlsVerify;
		}

		@Nullable String getTlsPath() {
			return this.tlsPath;
		}

		DockerContext withTlsPath(String tlsPath) {
			return new DockerContext(this.getNode(), tlsPath);
		}

		static DockerContext fromJson(String json) throws JsonProcessingException {
			return new DockerContext(SharedObjectMapper.get().readTree(json), null);
		}

		static DockerContext empty() {
			return new DockerContext(NullNode.instance, null);
		}

	}

}
