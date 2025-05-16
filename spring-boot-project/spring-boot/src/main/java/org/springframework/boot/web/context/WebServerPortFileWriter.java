/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.web.context;

import java.io.File;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.system.SystemProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationListener} that saves embedded server port and management port into
 * file. This application listener will be triggered whenever the server starts, and the
 * file name can be overridden at runtime with a System property or environment variable
 * named "PORTFILE" or "portfile".
 *
 * @author David Liu
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebServerPortFileWriter implements ApplicationListener<WebServerInitializedEvent> {

	private static final String DEFAULT_FILE_NAME = "application.port";

	private static final String[] PROPERTY_VARIABLES = { "PORTFILE", "portfile" };

	private static final Log logger = LogFactory.getLog(WebServerPortFileWriter.class);

	private final File file;

	/**
	 * Create a new {@link WebServerPortFileWriter} instance using the filename
	 * 'application.port'.
	 */
	public WebServerPortFileWriter() {
		this(new File(DEFAULT_FILE_NAME));
	}

	/**
	 * Create a new {@link WebServerPortFileWriter} instance with a specified filename.
	 * @param filename the name of file containing port
	 */
	public WebServerPortFileWriter(String filename) {
		this(new File(filename));
	}

	/**
	 * Create a new {@link WebServerPortFileWriter} instance with a specified file.
	 * @param file the file containing port
	 */
	public WebServerPortFileWriter(File file) {
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
	public void onApplicationEvent(WebServerInitializedEvent event) {
		File portFile = getPortFile(event.getApplicationContext());
		try {
			String port = String.valueOf(event.getWebServer().getPort());
			createParentDirectory(portFile);
			FileCopyUtils.copy(port.getBytes(), portFile);
			portFile.deleteOnExit();
		}
		catch (Exception ex) {
			logger.warn(LogMessage.format("Cannot create port file %s", this.file));
		}
	}

	/**
	 * Return the actual port file that should be written for the given application
	 * context. The default implementation builds a file from the source file and the
	 * application context namespace if available.
	 * @param applicationContext the source application context
	 * @return the file that should be written
	 */
	protected File getPortFile(ApplicationContext applicationContext) {
		String namespace = getServerNamespace(applicationContext);
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

	private String getServerNamespace(ApplicationContext applicationContext) {
		if (applicationContext instanceof WebServerApplicationContext webServerApplicationContext) {
			return webServerApplicationContext.getServerNamespace();
		}
		return null;
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
		if (parent != null) {
			parent.mkdirs();
		}
	}

}
