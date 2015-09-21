/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.maven;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.common.base.Strings;

/**
 * Debug an executable archive application on default port 5005 or the specified one.
 * @author Rishikesh Darandale
 */
@Mojo(name = "debug", requiresProject = true, defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class DebugMojo extends RunMojo {
	private static final String DEBUG_PORT = "5005";
	private static final String DEBUG_COMMAND = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=";
	/**
	 * Degug port that should be used to start the debugee process.
	 * @since 1.3
	 */
	@Parameter(property = "debug.port")
	private String debugPort;

	/**
	 * Return always true as we would be running debugee process as forked one.
	 * @return {@code true} if the application process should be forked
	 */
	@Override
	protected boolean isFork() {
		return true;
	}

	/**
	 * Resolve the JVM arguments to use.
	 * @return a {@link RunArguments} defining the JVM arguments
	 */
	@Override
	protected RunArguments resolveJvmArguments() {
		if (Strings.isNullOrEmpty(this.jvmArguments)) {
			this.jvmArguments = DEBUG_COMMAND + getDebugPort();
		}
		else {
			this.jvmArguments = this.jvmArguments + " " + DEBUG_COMMAND + getDebugPort();
		}
		return new RunArguments(this.jvmArguments);
	}

	private boolean hasDebugPort() {
		return (!Strings.isNullOrEmpty(this.debugPort));
	}

	private String getDebugPort() {
		return hasDebugPort() ? this.debugPort : DEBUG_PORT;
	}
}
