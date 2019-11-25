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

package org.springframework.boot.actuate.logging;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.logging.LogFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Web {@link Endpoint @Endpoint} that provides access to an application's log file.
 *
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@WebEndpoint(id = "logfile")
public class LogFileWebEndpoint {

	private static final Log logger = LogFactory.getLog(LogFileWebEndpoint.class);

	private File externalFile;

	private final LogFile logFile;

	public LogFileWebEndpoint(LogFile logFile, File externalFile) {
		this.externalFile = externalFile;
		this.logFile = logFile;
	}

	@ReadOperation(produces = "text/plain; charset=UTF-8")
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
		if (this.logFile == null) {
			logger.debug("Missing 'logging.file.name' or 'logging.file.path' properties");
			return null;
		}
		return new FileSystemResource(this.logFile.toString());
	}

}
