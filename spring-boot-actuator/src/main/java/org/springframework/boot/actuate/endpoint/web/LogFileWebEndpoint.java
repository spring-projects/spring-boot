/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointExposure;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.logging.LogFile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Web {@link Endpoint} that provides access to an application's log file.
 *
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "endpoints.logfile")
@Endpoint(id = "logfile", exposure = EndpointExposure.WEB)
public class LogFileWebEndpoint {

	private static final Log logger = LogFactory.getLog(LogFileWebEndpoint.class);

	private final Environment environment;

	/**
	 * External Logfile to be accessed. Can be used if the logfile is written by output
	 * redirect and not by the logging system itself.
	 */
	private File externalFile;

	public LogFileWebEndpoint(Environment environment) {
		this.environment = environment;
	}

	public File getExternalFile() {
		return this.externalFile;
	}

	public void setExternalFile(File externalFile) {
		this.externalFile = externalFile;
	}

	@ReadOperation
	public Resource logFile() {
		Resource logFileResource = getLogFileResource();
		if (logFileResource == null || !logFileResource.isReadable()) {
			return null;
		}
		return logFileResource;
	}

	private Resource getLogFileResource() {
		if (this.externalFile != null) {
			return new FileSystemResource(this.externalFile);
		}
		LogFile logFile = LogFile.get(this.environment);
		if (logFile == null) {
			logger.debug("Missing 'logging.file' or 'logging.path' properties");
			return null;
		}
		return new FileSystemResource(logFile.toString());
	}

}
