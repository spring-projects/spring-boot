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

package org.springframework.boot.actuate.system;

import java.io.File;

import org.springframework.context.ApplicationListener;

/**
 * An {@link ApplicationListener} that saves embedded server port and management port into
 * file. This application listener will be triggered whenever the servlet container
 * starts, and the file name can be overridden at runtime with a System property or
 * environment variable named "PORTFILE" or "portfile".
 *
 * @author David Liu
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 * @deprecated as of 1.4 in favor of
 * {@link org.springframework.boot.system.EmbeddedServerPortFileWriter}
 */
@Deprecated
public class EmbeddedServerPortFileWriter
		extends org.springframework.boot.system.EmbeddedServerPortFileWriter {

	/**
	 * Create a new {@link EmbeddedServerPortFileWriter} instance using the filename
	 * 'application.port'.
	 */
	public EmbeddedServerPortFileWriter() {
		super();
	}

	/**
	 * Create a new {@link EmbeddedServerPortFileWriter} instance with a specified
	 * filename.
	 * @param filename the name of file containing port
	 */
	public EmbeddedServerPortFileWriter(String filename) {
		super(filename);
	}

	/**
	 * Create a new {@link EmbeddedServerPortFileWriter} instance with a specified file.
	 * @param file the file containing port
	 */
	public EmbeddedServerPortFileWriter(File file) {
		super(file);
	}

}
