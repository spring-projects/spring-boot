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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.boot.buildpack.platform.system.Environment;

/**
 * Docker configuration stored in metadata files managed by the Docker CLI.
 *
 * @author Scott Frederick
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

	private final String configLocation;

	private final DockerConfig config;

	private final DockerContext context;

	/**
	 * Constructs a new DockerConfigurationMetadata object with the specified
	 * configuration location, DockerConfig, and DockerContext.
	 * @param configLocation the location of the Docker configuration
	 * @param config the Docker configuration
	 * @param context the Docker context
	 */
	private DockerConfigurationMetadata(String configLocation, DockerConfig config, DockerContext context) {
		this.configLocation = configLocation;
		this.config = config;
		this.context = context;
	}

	/**
	 * Retrieves the configuration of the DockerConfig object.
	 * @return the configuration of the DockerConfig object
	 */
	DockerConfig getConfiguration() {
		return this.config;
	}

	/**
	 * Returns the DockerContext associated with this DockerConfigurationMetadata.
	 * @return the DockerContext associated with this DockerConfigurationMetadata
	 */
	DockerContext getContext() {
		return this.context;
	}

	/**
	 * Creates a DockerContext object based on the provided context name.
	 * @param context the name of the context
	 * @return a DockerContext object representing the specified context
	 */
	DockerContext forContext(String context) {
		return createDockerContext(this.configLocation, context);
	}

	/**
	 * Creates a DockerConfigurationMetadata object from the given Environment.
	 * @param environment the Environment object containing the Docker configuration
	 * @return a DockerConfigurationMetadata object representing the Docker configuration
	 * metadata
	 */
	static DockerConfigurationMetadata from(Environment environment) {
		String configLocation = (environment.get(DOCKER_CONFIG) != null) ? environment.get(DOCKER_CONFIG)
				: Path.of(System.getProperty("user.home"), CONFIG_DIR).toString();
		DockerConfig dockerConfig = createDockerConfig(configLocation);
		DockerContext dockerContext = createDockerContext(configLocation, dockerConfig.getCurrentContext());
		return new DockerConfigurationMetadata(configLocation, dockerConfig, dockerContext);
	}

	/**
	 * Creates a DockerConfig object based on the provided configuration location.
	 * @param configLocation the location of the Docker configuration file
	 * @return a DockerConfig object representing the configuration
	 * @throws IllegalStateException if there is an error parsing the configuration file
	 */
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

	/**
	 * Creates a DockerContext object based on the provided configuration location and
	 * current context.
	 * @param configLocation the location of the Docker configuration
	 * @param currentContext the current Docker context
	 * @return a DockerContext object representing the current Docker context
	 * @throws IllegalArgumentException if the current context does not exist
	 * @throws IllegalStateException if there is an error parsing the Docker context
	 * metadata file
	 */
	private static DockerContext createDockerContext(String configLocation, String currentContext) {
		if (currentContext == null || DEFAULT_CONTEXT.equals(currentContext)) {
			return DockerContext.empty();
		}
		Path metaPath = Path.of(configLocation, CONTEXTS_DIR, META_DIR, asHash(currentContext), CONTEXT_FILE_NAME);
		Path tlsPath = Path.of(configLocation, CONTEXTS_DIR, TLS_DIR, asHash(currentContext), DOCKER_ENDPOINT);
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

	/**
	 * Generates a SHA-256 hash of the given current context.
	 * @param currentContext the current context to be hashed
	 * @return the SHA-256 hash of the current context as a hexadecimal string
	 * @throws NoSuchAlgorithmException if the SHA-256 algorithm is not available
	 */
	private static String asHash(String currentContext) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(currentContext.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			return null;
		}
	}

	/**
	 * Reads the content of a file specified by the given path.
	 * @param path the path of the file to read
	 * @return the content of the file as a string
	 * @throws IllegalStateException if an error occurs while reading the file
	 */
	private static String readPathContent(Path path) {
		try {
			return Files.readString(path);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Error reading Docker configuration file '" + path + "'", ex);
		}
	}

	/**
	 * DockerConfig class.
	 */
	static final class DockerConfig extends MappedObject {

		private final String currentContext;

		/**
		 * Constructs a new DockerConfig object with the provided JSON node.
		 * @param node the JSON node containing the Docker configuration data
		 */
		private DockerConfig(JsonNode node) {
			super(node, MethodHandles.lookup());
			this.currentContext = valueAt("/currentContext", String.class);
		}

		/**
		 * Returns the current context of the DockerConfig.
		 * @return the current context of the DockerConfig
		 */
		String getCurrentContext() {
			return this.currentContext;
		}

		/**
		 * Converts a JSON string representation of a DockerConfig object into a
		 * DockerConfig instance.
		 * @param json the JSON string representation of the DockerConfig object
		 * @return a DockerConfig instance created from the JSON string
		 * @throws JsonProcessingException if there is an error while processing the JSON
		 * string
		 */
		static DockerConfig fromJson(String json) throws JsonProcessingException {
			return new DockerConfig(SharedObjectMapper.get().readTree(json));
		}

		/**
		 * Creates an empty DockerConfig object.
		 * @return an empty DockerConfig object
		 */
		static DockerConfig empty() {
			return new DockerConfig(NullNode.instance);
		}

	}

	/**
	 * DockerContext class.
	 */
	static final class DockerContext extends MappedObject {

		private final String dockerHost;

		private final Boolean skipTlsVerify;

		private final String tlsPath;

		/**
		 * Constructs a new DockerContext object with the provided JSON node and TLS path.
		 * @param node the JSON node containing the Docker context information
		 * @param tlsPath the path to the TLS certificates for the Docker context
		 */
		private DockerContext(JsonNode node, String tlsPath) {
			super(node, MethodHandles.lookup());
			this.dockerHost = valueAt("/Endpoints/" + DOCKER_ENDPOINT + "/Host", String.class);
			this.skipTlsVerify = valueAt("/Endpoints/" + DOCKER_ENDPOINT + "/SkipTLSVerify", Boolean.class);
			this.tlsPath = tlsPath;
		}

		/**
		 * Returns the Docker host.
		 * @return the Docker host
		 */
		String getDockerHost() {
			return this.dockerHost;
		}

		/**
		 * Returns a boolean value indicating whether TLS verification is enabled or not.
		 * @return {@code true} if TLS verification is enabled, {@code false} otherwise
		 */
		Boolean isTlsVerify() {
			return this.skipTlsVerify != null && !this.skipTlsVerify;
		}

		/**
		 * Returns the path to the TLS directory.
		 * @return the path to the TLS directory
		 */
		String getTlsPath() {
			return this.tlsPath;
		}

		/**
		 * Creates a new DockerContext object with the specified TLS path.
		 * @param tlsPath the path to the TLS files
		 * @return a new DockerContext object with the specified TLS path
		 */
		DockerContext withTlsPath(String tlsPath) {
			return new DockerContext(this.getNode(), tlsPath);
		}

		/**
		 * Creates a DockerContext object from a JSON string.
		 * @param json the JSON string representing the DockerContext
		 * @return a DockerContext object created from the JSON string
		 * @throws JsonProcessingException if there is an error processing the JSON string
		 */
		static DockerContext fromJson(String json) throws JsonProcessingException {
			return new DockerContext(SharedObjectMapper.get().readTree(json), null);
		}

		/**
		 * Creates an empty DockerContext.
		 * @return an empty DockerContext object
		 */
		static DockerContext empty() {
			return new DockerContext(NullNode.instance, null);
		}

	}

}
