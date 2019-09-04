/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.cli.command.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.loader.tools.JarWriter;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * {@link Command} to create a self-contained executable jar file from a CLI application.
 *
 * @author Andrey Stolyarov
 * @author Phillip Webb
 * @author Henri Kerola
 * @since 1.3.0
 */
public class WarCommand extends ArchiveCommand {

	public WarCommand() {
		super("war", "Create a self-contained executable war file from a Spring Groovy script", new WarOptionHandler());
	}

	private static final class WarOptionHandler extends ArchiveOptionHandler {

		WarOptionHandler() {
			super("war", new Layouts.War());
		}

		@Override
		protected LibraryScope getLibraryScope(File file) {
			String fileName = file.getName();
			if (fileName.contains("tomcat-embed") || fileName.contains("spring-boot-starter-tomcat")) {
				return LibraryScope.PROVIDED;
			}
			return LibraryScope.COMPILE;
		}

		@Override
		protected void addCliClasses(JarWriter writer) throws IOException {
			addClass(writer, null, "org.springframework.boot.cli.app.SpringApplicationWebApplicationInitializer");
			super.addCliClasses(writer);
		}

		@Override
		protected void writeClasspathEntry(JarWriter writer, ResourceMatcher.MatchedResource entry) throws IOException {
			writer.writeEntry(getLayout().getClassesLocation() + entry.getName(), new FileInputStream(entry.getFile()));
		}

	}

}
