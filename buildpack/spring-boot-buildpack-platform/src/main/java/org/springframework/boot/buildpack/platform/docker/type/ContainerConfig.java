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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.boot.buildpack.platform.json.SharedJsonMapper;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration used when creating a new container.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @since 2.3.0
 */
public class ContainerConfig {

	private final String json;

	ContainerConfig(@Nullable String user, ImageReference image, String command, List<String> args,
			Map<String, String> labels, List<Binding> bindings, Map<String, String> env, @Nullable String networkMode,
			List<String> securityOptions) {
		Assert.notNull(image, "'image' must not be null");
		Assert.hasText(command, "'command' must not be empty");
		JsonMapper jsonMapper = SharedJsonMapper.get();
		ObjectNode node = jsonMapper.createObjectNode();
		if (StringUtils.hasText(user)) {
			node.put("User", user);
		}
		node.put("Image", image.toString());
		ArrayNode commandNode = node.putArray("Cmd");
		commandNode.add(command);
		args.forEach(commandNode::add);
		ArrayNode envNode = node.putArray("Env");
		env.forEach((name, value) -> envNode.add(name + "=" + value));
		ObjectNode labelsNode = node.putObject("Labels");
		labels.forEach(labelsNode::put);
		ObjectNode hostConfigNode = node.putObject("HostConfig");
		if (networkMode != null) {
			hostConfigNode.put("NetworkMode", networkMode);
		}
		ArrayNode bindsNode = hostConfigNode.putArray("Binds");
		bindings.forEach((binding) -> bindsNode.add(binding.toString()));
		if (!CollectionUtils.isEmpty(securityOptions)) {
			ArrayNode securityOptsNode = hostConfigNode.putArray("SecurityOpt");
			securityOptions.forEach(securityOptsNode::add);
		}
		this.json = jsonMapper.writeValueAsString(node);
	}

	/**
	 * Write this container configuration to the specified {@link OutputStream}.
	 * @param outputStream the output stream
	 * @throws IOException on IO error
	 */
	public void writeTo(OutputStream outputStream) throws IOException {
		StreamUtils.copy(this.json, StandardCharsets.UTF_8, outputStream);
	}

	@Override
	public String toString() {
		return this.json;
	}

	/**
	 * Factory method to create a {@link ContainerConfig} with specific settings.
	 * @param imageReference the source image for the container config
	 * @param update an update callback used to customize the config
	 * @return a new {@link ContainerConfig} instance
	 */
	public static ContainerConfig of(ImageReference imageReference, Consumer<Update> update) {
		Assert.notNull(imageReference, "'imageReference' must not be null");
		Assert.notNull(update, "'update' must not be null");
		return new Update(imageReference).run(update);
	}

	/**
	 * Update class used to change data when creating a container config.
	 */
	public static class Update {

		private final ImageReference image;

		private @Nullable String user;

		private @Nullable String command;

		private final List<String> args = new ArrayList<>();

		private final Map<String, String> labels = new LinkedHashMap<>();

		private final List<Binding> bindings = new ArrayList<>();

		private final Map<String, String> env = new LinkedHashMap<>();

		private @Nullable String networkMode;

		private final List<String> securityOptions = new ArrayList<>();

		Update(ImageReference image) {
			this.image = image;
		}

		private ContainerConfig run(Consumer<Update> update) {
			update.accept(this);
			Assert.state(this.command != null, "'command' must not be null");
			return new ContainerConfig(this.user, this.image, this.command, this.args, this.labels, this.bindings,
					this.env, this.networkMode, this.securityOptions);
		}

		/**
		 * Update the container config with a specific user.
		 * @param user the user to set
		 */
		public void withUser(String user) {
			this.user = user;
		}

		/**
		 * Update the container config with a specific command.
		 * @param command the command to set
		 * @param args additional arguments to add
		 * @see #withArgs(String...)
		 */
		public void withCommand(String command, String... args) {
			this.command = command;
			withArgs(args);
		}

		/**
		 * Update the container config with additional args.
		 * @param args the arguments to add
		 */
		public void withArgs(String... args) {
			this.args.addAll(Arrays.asList(args));
		}

		/**
		 * Update the container config with an additional label.
		 * @param name the label name
		 * @param value the label value
		 */
		public void withLabel(String name, String value) {
			this.labels.put(name, value);
		}

		/**
		 * Update the container config with an additional binding.
		 * @param binding the binding
		 */
		public void withBinding(Binding binding) {
			this.bindings.add(binding);
		}

		/**
		 * Update the container config with an additional environment variable.
		 * @param name the variable name
		 * @param value the variable value
		 */
		public void withEnv(String name, String value) {
			this.env.put(name, value);
		}

		/**
		 * Update the container config with the network that the build container will
		 * connect to.
		 * @param networkMode the network
		 */
		public void withNetworkMode(@Nullable String networkMode) {
			this.networkMode = networkMode;
		}

		/**
		 * Update the container config with a security option.
		 * @param option the security option
		 */
		public void withSecurityOption(String option) {
			this.securityOptions.add(option);
		}

	}

}
