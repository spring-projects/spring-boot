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

package org.springframework.boot.cli.command.archive;

import java.io.File;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * {@link Command} to create a self-contained executable jar file from a CLI application.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class JarCommand extends ArchiveCommand {

	public JarCommand() {
		super("jar", "Create a self-contained executable jar "
				+ "file from a Spring Groovy script", new JarOptionHandler());
	}

	private static final class JarOptionHandler extends ArchiveOptionHandler {

		JarOptionHandler() {
			super("jar", new Layouts.Jar());
		}

		@Override
		protected LibraryScope getLibraryScope(File file) {
			return LibraryScope.COMPILE;
		}

	}

}
