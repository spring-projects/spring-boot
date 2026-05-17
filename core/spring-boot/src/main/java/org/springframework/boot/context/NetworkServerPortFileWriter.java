/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context;

import java.io.File;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.PortBound;
import org.springframework.boot.system.SystemProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationListener} that saves bound network ports into a file. Useful for
 * obtaining the local port of a running server that publishes a {@link PortBound} event.
 *
 * @author Somil Jain
 * @since 4.0.0
 */
public class NetworkServerPortFileWriter implements ApplicationListener<ApplicationEvent> {

	private static final String DEFAULT_FILE_NAME = "application.port";

	private static final String[] PROPERTY_VARIABLES = { "PORTFILE", "portfile" };

	private static final Log logger = LogFactory.getLog(NetworkServerPortFileWriter.class);

	private final File file;

	/**
	 * Create a new {@link NetworkServerPortFileWriter} instance using the filename
	 * {@code application.port}.
	 */
	public NetworkServerPortFileWriter() {
		this(new File(DEFAULT_FILE_NAME));
	}

	/**
	 * Create a new {@link NetworkServerPortFileWriter} instance with a specified
	 * filename.
	 * @param filename the name of the file containing the port
	 */
	public NetworkServerPortFileWriter(String filename) {
		this(new File(filename));
	}

	/**
	 * Create a new {@link NetworkServerPortFileWriter} instance with a specified file.
	 * @param file the file containing the port
	 */
	public NetworkServerPortFileWriter(File file) {
		Assert.notNull(file, "'file' must not be null");
		String override = SystemProperties.get(PROPERTY_VARIABLES);
		if (override != null) {
			this.file = new File(override);
		}
		else {
			this.file = file;
		}
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof PortBound portBoundEvent) {
			File portFile = getPortFile(portBoundEvent);
			try {
				String port = String.valueOf(portBoundEvent.getPort());
				createParentDirectory(portFile);
				FileCopyUtils.copy(port.getBytes(), portFile);
				portFile.deleteOnExit();
			}
			catch (Exception ex) {
				logger.warn(LogMessage.format("Cannot create port file %s", portFile));
			}
		}
	}

	/**
	 * Return the port file to write for the given event.
	 * @param event the port bound event
	 * @return the port file
	 */
	protected File getPortFile(PortBound event) {
		String namespace = event.getNamespace();
		if (!StringUtils.hasLength(namespace)) {
			return this.file;
		}
		String filename = this.file.getName();
		String extension = StringUtils.getFilenameExtension(filename);
		String filenameWithoutExtension = (extension != null)
				? filename.substring(0, filename.length() - extension.length() - 1) : filename;
		String suffix = (!isUpperCase(filename)) ? namespace.toLowerCase(Locale.ENGLISH)
				: namespace.toUpperCase(Locale.ENGLISH);
		return new File(this.file.getParentFile(),
				filenameWithoutExtension + "-" + suffix + ((!StringUtils.hasLength(extension)) ? "" : "." + extension));
	}

	private boolean isUpperCase(String name) {
		for (int i = 0; i < name.length(); i++) {
			if (Character.isLetter(name.charAt(i)) && !Character.isUpperCase(name.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private void createParentDirectory(File file) {
		File parent = file.getParentFile();
		if (parent != null && !parent.mkdirs() && !parent.exists()) {
			throw new IllegalStateException("Cannot create parent directory " + parent);
		}
	}

}
