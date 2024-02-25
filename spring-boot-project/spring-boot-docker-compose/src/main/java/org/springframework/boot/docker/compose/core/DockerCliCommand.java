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

package org.springframework.boot.docker.compose.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

	private final List<String> command;

	/**
     * Constructs a new DockerCliCommand with the specified parameters.
     * 
     * @param type the type of the command
     * @param responseType the class representing the response type
     * @param listResponse true if the response is a list, false otherwise
     * @param command the command to be executed
     */
    private DockerCliCommand(Type type, Class<?> responseType, boolean listResponse, String... command) {
		this(type, LogLevel.OFF, responseType, listResponse, command);
	}

	/**
     * Creates a new instance of DockerCliCommand.
     * 
     * @param type the type of the command
     * @param logLevel the log level for the command
     * @param responseType the response type for the command
     * @param listResponse indicates whether the response is a list
     * @param command the command to be executed
     */
    private DockerCliCommand(Type type, LogLevel logLevel, Class<?> responseType, boolean listResponse,
			String... command) {
		this.type = type;
		this.logLevel = logLevel;
		this.responseType = responseType;
		this.listResponse = listResponse;
		this.command = List.of(command);
	}

	/**
     * Returns the type of the Docker CLI command.
     *
     * @return the type of the Docker CLI command
     */
    Type getType() {
		return this.type;
	}

	/**
     * Returns the log level of the Docker CLI command.
     *
     * @return the log level of the Docker CLI command
     */
    LogLevel getLogLevel() {
		return this.logLevel;
	}

	/**
     * Returns the command list.
     *
     * @return the command list
     */
    List<String> getCommand() {
		return this.command;
	}

	/**
     * Deserializes the given JSON string into an object of type R.
     * 
     * @param json the JSON string to be deserialized
     * @return the deserialized object of type R, or null if the responseType is Void.class
     * @throws DockerJsonException if there is an error during deserialization
     */
    @SuppressWarnings("unchecked")
	R deserialize(String json) {
		if (this.responseType == Void.class) {
			return null;
		}
		return (R) ((!this.listResponse) ? DockerJson.deserialize(json, this.responseType)
				: DockerJson.deserializeToList(json, this.responseType));
	}

	/**
     * Compares this DockerCliCommand object to the specified object for equality.
     * Returns true if the specified object is also a DockerCliCommand and all the
     * fields of both objects are equal.
     *
     * @param obj the object to compare this DockerCliCommand against
     * @return true if the given object represents a DockerCliCommand equivalent to this command, false otherwise
     */
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
		result = result && this.command.equals(other.command);
		return result;
	}

	/**
     * Returns the hash code value for the DockerCliCommand object.
     * 
     * @return the hash code value for the DockerCliCommand object
     */
    @Override
	public int hashCode() {
		return Objects.hash(this.type, this.responseType, this.listResponse, this.command);
	}

	/**
     * Returns a string representation of the DockerCliCommand object.
     * 
     * @return a string representation of the DockerCliCommand object
     */
    @Override
	public String toString() {
		return "DockerCliCommand [type=%s, responseType=%s, listResponse=%s, command=%s]".formatted(this.type,
				this.responseType, this.listResponse, this.command);
	}

	/**
     * Joins the given command and arguments into a single array of strings.
     *
     * @param command the collection of command strings
     * @param args the collection of argument strings
     * @return an array of strings containing the joined command and arguments
     */
    protected static String[] join(Collection<String> command, Collection<String> args) {
		List<String> result = new ArrayList<>(command);
		result.addAll(args);
		return result.toArray(new String[0]);
	}

	/**
	 * The {@code docker context} command.
	 */
	static final class Context extends DockerCliCommand<List<DockerCliContextResponse>> {

		/**
         * Constructor for Context class.
         * 
         * @param type the type of the context (in this case, Type.DOCKER)
         * @param responseType the response type of the context (in this case, DockerCliContextResponse.class)
         * @param isAsync boolean value indicating if the context is asynchronous or not
         * @param command the command to be executed in the context (in this case, "context")
         * @param args the arguments to be passed to the command (in this case, "ls", "--format={{ json . }}")
         */
        Context() {
			super(Type.DOCKER, DockerCliContextResponse.class, true, "context", "ls", "--format={{ json . }}");
		}

	}

	/**
	 * The {@code docker inspect} command.
	 */
	static final class Inspect extends DockerCliCommand<List<DockerCliInspectResponse>> {

		/**
         * Inspects the Docker containers with the given IDs.
         * 
         * @param ids the collection of container IDs to inspect
         * @throws DockerCliException if there is an error executing the Docker CLI command
         * @return the Docker CLI inspect response containing the inspection results
         */
        Inspect(Collection<String> ids) {
			super(Type.DOCKER, DockerCliInspectResponse.class, true,
					join(List.of("inspect", "--format={{ json . }}"), ids));
		}

	}

	/**
	 * The {@code docker compose config} command.
	 */
	static final class ComposeConfig extends DockerCliCommand<DockerCliComposeConfigResponse> {

		/**
         * Creates a new instance of ComposeConfig.
         * 
         * @param type the type of the configuration
         * @param responseType the response type for the configuration
         * @param isListCommand true if the configuration is for a list command, false otherwise
         * @param args the arguments for the configuration
         */
        ComposeConfig() {
			super(Type.DOCKER_COMPOSE, DockerCliComposeConfigResponse.class, false, "config", "--format=json");
		}

	}

	/**
	 * The {@code docker compose ps} command.
	 */
	static final class ComposePs extends DockerCliCommand<List<DockerCliComposePsResponse>> {

		/**
         * Constructor for ComposePs class.
         * Initializes the command type as Type.DOCKER_COMPOSE,
         * the response type as DockerCliComposePsResponse.class,
         * sets the command to be executed as "ps",
         * and sets the format option to "--format=json".
         */
        ComposePs() {
			super(Type.DOCKER_COMPOSE, DockerCliComposePsResponse.class, true, "ps", "--format=json");
		}

	}

	/**
	 * The {@code docker compose up} command.
	 */
	static final class ComposeUp extends DockerCliCommand<Void> {

		/**
         * Creates a new instance of ComposeUp with the specified log level.
         * 
         * @param logLevel the log level to be used for logging
         */
        ComposeUp(LogLevel logLevel) {
			super(Type.DOCKER_COMPOSE, logLevel, Void.class, false, "up", "--no-color", "--detach", "--wait");
		}

	}

	/**
	 * The {@code docker compose down} command.
	 */
	static final class ComposeDown extends DockerCliCommand<Void> {

		/**
         * Shuts down the Docker Compose environment.
         * 
         * @param timeout the duration to wait for the shutdown process to complete
         */
        ComposeDown(Duration timeout) {
			super(Type.DOCKER_COMPOSE, Void.class, false, "down", "--timeout", Long.toString(timeout.toSeconds()));
		}

	}

	/**
	 * The {@code docker compose start} command.
	 */
	static final class ComposeStart extends DockerCliCommand<Void> {

		/**
         * Constructs a new instance of the ComposeStart class with the specified log level.
         * 
         * @param logLevel the log level to be used for logging
         */
        ComposeStart(LogLevel logLevel) {
			super(Type.DOCKER_COMPOSE, logLevel, Void.class, false, "start");
		}

	}

	/**
	 * The {@code docker compose stop} command.
	 */
	static final class ComposeStop extends DockerCliCommand<Void> {

		/**
         * Constructs a new ComposeStop command with the specified timeout.
         * 
         * @param timeout the duration after which the command will timeout
         */
        ComposeStop(Duration timeout) {
			super(Type.DOCKER_COMPOSE, Void.class, false, "stop", "--timeout", Long.toString(timeout.toSeconds()));
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

}
