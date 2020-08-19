/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.buildpack.platform.docker.type.ContainerConfig;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.util.StringUtils;

/**
 * An individual build phase executed as part of a {@link Lifecycle} run.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class Phase {

	private static final String DOMAIN_SOCKET_PATH = "/var/run/docker.sock";

	private final String name;

	private final boolean verboseLogging;

	private boolean daemonAccess = false;

	private final List<String> args = new ArrayList<>();

	private final Map<VolumeName, String> binds = new LinkedHashMap<>();

	/**
	 * Create a new {@link Phase} instance.
	 * @param name the name of the phase
	 * @param verboseLogging if verbose logging is requested
	 */
	Phase(String name, boolean verboseLogging) {
		this.name = name;
		this.verboseLogging = verboseLogging;
	}

	/**
	 * Update this phase with Docker daemon access.
	 */
	void withDaemonAccess() {
		this.daemonAccess = true;
	}

	/**
	 * Update this phase with a debug log level arguments if verbose logging has been
	 * requested.
	 */
	void withLogLevelArg() {
		if (this.verboseLogging) {
			this.args.add("-log-level");
			this.args.add("debug");
		}
	}

	/**
	 * Update this phase with additional run arguments.
	 * @param args the arguments to add
	 */
	void withArgs(Object... args) {
		Arrays.stream(args).map(Object::toString).forEach(this.args::add);
	}

	/**
	 * Update this phase with an addition volume binding.
	 * @param source the source volume
	 * @param dest the destination location
	 */
	void withBinds(VolumeName source, String dest) {
		this.binds.put(source, dest);
	}

	/**
	 * Return the name of the phase.
	 * @return the phase name
	 */
	String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * Apply this phase settings to a {@link ContainerConfig} update.
	 * @param update the update to apply the phase to
	 */
	void apply(ContainerConfig.Update update) {
		if (this.daemonAccess) {
			update.withUser("root");
			update.withBind(DOMAIN_SOCKET_PATH, DOMAIN_SOCKET_PATH);
		}
		update.withCommand("/cnb/lifecycle/" + this.name, StringUtils.toStringArray(this.args));
		update.withLabel("author", "spring-boot");
		this.binds.forEach(update::withBind);
	}

}
