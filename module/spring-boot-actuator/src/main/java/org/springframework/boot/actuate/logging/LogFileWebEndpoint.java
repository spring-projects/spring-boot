/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.logging;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

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

	private final @Nullable LogFile logFile;

	private final @Nullable File externalFile;

	public LogFileWebEndpoint(@Nullable LogFile logFile, @Nullable File externalFile) {
		this.logFile = logFile;
		this.externalFile = externalFile;
	}

	@ReadOperation(produces = "text/plain; charset=UTF-8")
	public @Nullable Resource logFile() {
		Resource logFileResource = getLogFileResource();
		if (logFileResource == null || !logFileResource.isReadable()) {
			return null;
		}
		return logFileResource;
	}

	private @Nullable Resource getLogFileResource() {
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
