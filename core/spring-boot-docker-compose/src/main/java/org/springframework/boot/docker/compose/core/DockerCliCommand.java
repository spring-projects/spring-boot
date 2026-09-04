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

package org.springframework.boot.docker.compose.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.boot.logging.LogLevel;

/**
 * Commands that can be executed by the {@link DockerCli}.
 *
 * @param <R> the response type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract sealed class DockerCliCommand<R> {

	private final Type type;

	private final LogLevel logLevel;

	private final Class<?> responseType;

	private final boolean listResponse;

	private final Function<ComposeContext, List<String>> command;

	private DockerCliCommand(Type type, Class<?> responseType, boolean listResponse, String... command) {
		this(type, LogLevel.OFF, responseType, listResponse, command);
	}

	private DockerCliCommand(Type type, LogLevel logLevel, Class<?> responseType, boolean listResponse,
			String... command) {
		this(type, logLevel, responseType, listResponse, (context) -> List.of(command));
	}

	private DockerCliCommand(Type type, LogLevel logLevel, Class<?> responseType, boolean listResponse,
			Function<ComposeContext, List<String>> command) {
		this.type = type;
		this.logLevel = logLevel;
		this.responseType = responseType;
		this.listResponse = listResponse;
		this.command = command;
	}

	Type getType() {
		return this.type;
	}

	LogLevel getLogLevel() {
		return this.logLevel;
	}

	List<String> getCommand(ComposeContext context) {
		return this.command.apply(context);
	}

	boolean isSupported(ComposeContext context) {
		return true;
	}

	R emptyResponse() {
		throw new UnsupportedOperationException("Command is not supported and provides no empty response");
	}

	@SuppressWarnings("unchecked")
	R convert(String response, ComposeContext context) {
		if (this.responseType == None.class) {
			return (R) None.INSTANCE;
		}
		if (this.responseType == String.class) {
			return (R) response;
		}
		return (R) ((!this.listResponse) ? DockerJson.deserialize(response, this.responseType)
				: DockerJson.deserializeToList(response, this.responseType));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DockerCliCommand<?> other = (DockerCliCommand<?>) obj;
		boolean result = this.type == other.type;
		result = result && this.responseType == other.responseType;
		result = result && this.listResponse == other.listResponse;
		result = result
				&& this.command.apply(ComposeContext.UNKNOWN).equals(other.command.apply(ComposeContext.UNKNOWN));
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.responseType, this.listResponse, this.command);
	}

	@Override
	public String toString() {
		return "DockerCliCommand [type=%s, responseType=%s, listResponse=%s, command=%s]".formatted(this.type,
				this.responseType, this.listResponse, this.command);
	}

	protected static String[] join(Collection<String> command, Collection<String> args) {
		List<String> result = new ArrayList<>(command);
		result.addAll(args);
		return result.toArray(new String[0]);
	}

	/**
	 * The {@code docker context} command.
	 */
	static final class Context extends DockerCliCommand<List<DockerCliContextResponse>> {

		Context() {
			super(Type.DOCKER, DockerCliContextResponse.class, true, "context", "ls", "--format={{ json . }}");
		}

		@Override
		boolean isSupported(ComposeContext context) {
			return context.engine() == ContainerEngine.DOCKER;
		}

		@Override
		List<DockerCliContextResponse> emptyResponse() {
			return java.util.Collections.emptyList();
		}

	}

	/**
	 * The {@code docker inspect} command.
	 */
	static final class Inspect extends DockerCliCommand<List<DockerCliInspectResponse>> {

		Inspect(Collection<String> ids) {
			super(Type.DOCKER, DockerCliInspectResponse.class, true,
					join(List.of("inspect", "--format={{ json . }}"), ids));
		}

	}

	/**
	 * The {@code docker compose config} command.
	 */
	static final class ComposeConfig extends DockerCliCommand<DockerCliComposeConfigResponse> {

		private static final List<String> DOCKER_COMMAND = List.of("config", "--format=json");

		private static final List<String> PODMAN_COMMAND = List.of("config");

		ComposeConfig() {
			super(Type.DOCKER_COMPOSE, LogLevel.OFF, DockerCliComposeConfigResponse.class, false,
					ComposeConfig::getConfigCommand);
		}

		private static List<String> getConfigCommand(ComposeContext context) {
			return (context.engine() == ContainerEngine.PODMAN) ? PODMAN_COMMAND : DOCKER_COMMAND;
		}

		@Override
		DockerCliComposeConfigResponse convert(String response, ComposeContext context) {
			if (context.engine() == ContainerEngine.PODMAN) {
				return PodmanComposeConfigYamlParser.parse(response);
			}
			return super.convert(response, context);
		}

	}

	/**
	 * The {@code docker compose ps} command.
	 */
	static final class ComposePs extends DockerCliCommand<List<DockerCliComposePsResponse>> {

		private static final List<String> WITHOUT_ORPHANS = List.of("ps", "--format=json");

		private static final List<String> WITH_ORPHANS = List.of("ps", "--orphans=false", "--format=json");

		ComposePs() {
			super(Type.DOCKER_COMPOSE, LogLevel.OFF, DockerCliComposePsResponse.class, true, ComposePs::getPsCommand);
		}

		private static List<String> getPsCommand(ComposeContext context) {
			if (context.engine() == ContainerEngine.DOCKER && !context.composeVersion().isLessThan(2, 24)) {
				return WITH_ORPHANS;
			}
			return WITHOUT_ORPHANS;
		}

	}

	/**
	 * The {@code docker compose up} command.
	 */
	static final class ComposeUp extends DockerCliCommand<None> {

		ComposeUp(LogLevel logLevel, List<String> arguments) {
			super(Type.DOCKER_COMPOSE, logLevel, None.class, false, (context) -> getCommand(context, arguments));
		}

		private static List<String> getCommand(ComposeContext context, List<String> arguments) {
			List<String> result = new ArrayList<>();
			result.add("up");
			result.add("--no-color");
			result.add("--detach");
			if (context.engine() == ContainerEngine.DOCKER) {
				result.add("--wait");
			}
			result.addAll(arguments);
			return result;
		}

	}

	/**
	 * The {@code docker compose down} command.
	 */
	static final class ComposeDown extends DockerCliCommand<None> {

		ComposeDown(Duration timeout, List<String> arguments) {
			super(Type.DOCKER_COMPOSE, None.class, false, getCommand(timeout, arguments));
		}

		private static String[] getCommand(Duration timeout, List<String> arguments) {
			List<String> command = new ArrayList<>();
			command.add("down");
			command.add("--timeout");
			command.add(Long.toString(timeout.toSeconds()));
			command.addAll(arguments);
			return command.toArray(String[]::new);
		}

	}

	/**
	 * The {@code docker compose start} command.
	 */
	static final class ComposeStart extends DockerCliCommand<None> {

		ComposeStart(LogLevel logLevel, List<String> arguments) {
			super(Type.DOCKER_COMPOSE, logLevel, None.class, false, getCommand(arguments));
		}

		private static String[] getCommand(List<String> arguments) {
			List<String> command = new ArrayList<>();
			command.add("start");
			command.addAll(arguments);
			return command.toArray(String[]::new);
		}

	}

	/**
	 * The {@code docker compose stop} command.
	 */
	static final class ComposeStop extends DockerCliCommand<None> {

		ComposeStop(Duration timeout, List<String> arguments) {
			super(Type.DOCKER_COMPOSE, None.class, false, getCommand(timeout, arguments));
		}

		private static String[] getCommand(Duration timeout, List<String> arguments) {
			List<String> command = new ArrayList<>();
			command.add("stop");
			command.add("--timeout");
			command.add(Long.toString(timeout.toSeconds()));
			command.addAll(arguments);
			return command.toArray(String[]::new);
		}

	}

	/**
	 * The {@code docker compose logs} command.
	 */
	static final class ComposeLogs extends DockerCliCommand<String> {

		ComposeLogs() {
			super(Type.DOCKER_COMPOSE, String.class, false, "logs");
		}

	}

	/**
	 * Command Types.
	 */
	enum Type {

		/**
		 * A command executed using {@code docker}.
		 */
		DOCKER,

		/**
		 * A command executed using {@code docker compose} or {@code docker-compose}.
		 */
		DOCKER_COMPOSE

	}

	static final class None {

		public static final None INSTANCE = new None();

		private None() {
		}

	}

	/**
	 * Docker compose version.
	 *
	 * @param major the major component
	 * @param minor the minor component
	 */
	record ComposeVersion(int major, int minor) {

		static final ComposeVersion UNKNOWN = new ComposeVersion(0, 0);

		boolean isLessThan(int major, int minor) {
			return major() < major || major() == major && minor() < minor;
		}

		static ComposeVersion of(String value) {
			try {
				value = (!value.toLowerCase(Locale.ROOT).startsWith("v")) ? value : value.substring(1);
				String[] parts = value.split("\\.");
				return new ComposeVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			}
			catch (Exception ex) {
				return UNKNOWN;
			}
		}

	}

	/**
	 * The active compose context, combining the container engine with the compose
	 * version.
	 *
	 * @param engine the active container engine
	 * @param composeVersion the compose version
	 */
	record ComposeContext(ContainerEngine engine, ComposeVersion composeVersion) {

		static final ComposeContext UNKNOWN = new ComposeContext(ContainerEngine.DOCKER, ComposeVersion.UNKNOWN);

	}

}
