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

package org.springframework.boot.cli.command;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joptsimple.OptionSet;

/**
 * Extract file options (anything following '--' in an {@link OptionSet}).
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @author Greg Turnquist
 */
public class FileOptions {

	private List<File> files;

	private List<?> args;

	/**
	 * Create a new {@link FileOptions} instance.
	 * @param options the source option set
	 */
	public FileOptions(OptionSet options) {
		this(options, null);
	}

	/**
	 * Create a new {@link FileOptions} instance.
	 * @param optionSet the source option set
	 * @param classLoader an optional classloader used to try and load files that are not
	 * found directly.
	 */
	public FileOptions(OptionSet optionSet, ClassLoader classLoader) {
		List<?> nonOptionArguments = optionSet.nonOptionArguments();
		List<File> files = new ArrayList<File>();
		for (Object option : nonOptionArguments) {
			if (option instanceof String) {
				String filename = (String) option;
				if ("--".equals(filename)) {
					break;
				}
				if (filename.endsWith(".groovy") || filename.endsWith(".java")) {
					File file = getFile(filename, classLoader);
					if (file == null) {
						throw new RuntimeException("Can't find " + filename);
					}
					files.add(file);
				}
			}
		}
		if (files.size() == 0) {
			throw new RuntimeException("Please specify a file to run");
		}
		this.files = Collections.unmodifiableList(files);
		this.args = Collections.unmodifiableList(nonOptionArguments.subList(files.size(),
				nonOptionArguments.size()));
	}

	private File getFile(String filename, ClassLoader classLoader) {
		File file = new File(filename);
		if (file.isFile() && file.canRead()) {
			return file;
		}
		if (classLoader != null) {
			URL url = classLoader.getResource(filename);
			if (url != null && url.toString().startsWith("file:")) {
				return new File(url.toString().substring("file:".length()));
			}
		}
		return null;
	}

	public List<?> getArgs() {
		return this.args;
	}

	public String[] getArgsArray() {
		return this.args.toArray(new String[this.args.size()]);
	}

	public List<File> getFiles() {
		return this.files;
	}

	public File[] getFilesArray() {
		return this.files.toArray(new File[this.files.size()]);
	}

}
