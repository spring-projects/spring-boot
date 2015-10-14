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

package org.springframework.boot.cli.command.war;


import org.springframework.boot.cli.command.ArchiveCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * {@link Command} to create a self-contained executable jar file from a CLI application.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Andrey Stolyarov
 */
public class WarCommand extends ArchiveCommand {

	public WarCommand() {
		super("war", "Create a self-contained "
						+ "executable war file from a Spring Groovy script",
				new ArchiveCommand.LibraryScopeResolver() {
					@Override
					public LibraryScope resolve(String fileName) {
						if (fileName.contains("tomcat-embed") ||
								fileName.contains("spring-boot-starter-tomcat")) {
							return  LibraryScope.PROVIDED;
						}
						else {
							return  LibraryScope.COMPILE;
						}
					}
				},
				new Layouts.War()
		);
	}

}
