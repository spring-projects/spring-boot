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

package org.springframework.boot.actuate.startup;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.startup.StartupEndpoint.StartupEndpointRuntimeHints;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.metrics.buffering.StartupTimeline;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * {@link Endpoint @Endpoint} to expose the timeline of the
 * {@link org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup
 * application startup}.
 *
 * @author Brian Clozel
 * @author Chris Bono
 * @since 2.4.0
 */
@Endpoint(id = "startup")
@ImportRuntimeHints(StartupEndpointRuntimeHints.class)
public class StartupEndpoint {

	private final BufferingApplicationStartup applicationStartup;

	/**
	 * Creates a new {@code StartupEndpoint} that will describe the timeline of buffered
	 * application startup events.
	 * @param applicationStartup the application startup
	 */
	public StartupEndpoint(BufferingApplicationStartup applicationStartup) {
		this.applicationStartup = applicationStartup;
	}

	@ReadOperation
	public StartupResponse startupSnapshot() {
		StartupTimeline startupTimeline = this.applicationStartup.getBufferedTimeline();
		return new StartupResponse(startupTimeline);
	}

	@WriteOperation
	public StartupResponse startup() {
		StartupTimeline startupTimeline = this.applicationStartup.drainBufferedTimeline();
		return new StartupResponse(startupTimeline);
	}

	/**
	 * A description of an application startup, primarily intended for serialization to
	 * JSON.
	 */
	public static final class StartupResponse {

		private final String springBootVersion;

		private final StartupTimeline timeline;

		private StartupResponse(StartupTimeline timeline) {
			this.timeline = timeline;
			this.springBootVersion = SpringBootVersion.getVersion();
		}

		public String getSpringBootVersion() {
			return this.springBootVersion;
		}

		public StartupTimeline getTimeline() {
			return this.timeline;
		}

	}

	static class StartupEndpointRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection().registerType(
					TypeReference
							.of("org.springframework.boot.context.metrics.buffering.BufferedStartupStep$DefaultTag"),
					(hint) -> hint
							.onReachableType(TypeReference
									.of("org.springframework.boot.context.metrics.buffering.BufferedStartupStep"))
							.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
			hints.reflection().registerType(
					TypeReference
							.of("org.springframework.core.metrics.jfr.FlightRecorderStartupStep$FlightRecorderTag"),
					(hint) -> hint
							.onReachableType(
									TypeReference.of("org.springframework.core.metrics.jfr.FlightRecorderStartupStep"))
							.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
		}

	}

}
