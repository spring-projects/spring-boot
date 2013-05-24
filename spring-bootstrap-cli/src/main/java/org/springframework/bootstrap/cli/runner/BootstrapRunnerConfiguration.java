/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.cli.runner;

import java.util.logging.Level;

import org.springframework.bootstrap.cli.compiler.GroovyCompilerConfiguration;

/**
 * Configuration for the {@link BootstrapRunner}.
 * 
 * @author Phillip Webb
 */
public interface BootstrapRunnerConfiguration extends GroovyCompilerConfiguration {

	/**
	 * Returns {@code true} if the source file should be monitored for changes and
	 * automatically recompiled.
	 */
	boolean isWatchForFileChanges();

	/**
	 * Returns the logging level to use.
	 */
	Level getLogLevel();

	/**
	 * Returns {@code true} if the dependencies should be cached locally
	 */
	boolean isLocal();

}
