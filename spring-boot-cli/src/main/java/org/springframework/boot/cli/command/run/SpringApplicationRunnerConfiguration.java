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

package org.springframework.boot.cli.command.run;

import java.util.logging.Level;

import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;

/**
 * Configuration for the {@link SpringApplicationRunner}.
 *
 * @author Phillip Webb
 */
public interface SpringApplicationRunnerConfiguration
		extends GroovyCompilerConfiguration {

	/**
	 * Returns {@code true} if the source file should be monitored for changes and
	 * automatically recompiled.
	 * @return {@code true} if file watching should be performed, otherwise {@code false}
	 */
	boolean isWatchForFileChanges();

	/**
	 * Returns the logging level to use.
	 * @return the logging level
	 */
	Level getLogLevel();
}
