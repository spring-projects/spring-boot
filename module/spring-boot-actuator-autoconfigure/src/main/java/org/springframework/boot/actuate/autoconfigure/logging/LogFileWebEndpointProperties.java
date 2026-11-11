/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.logging;

import java.io.File;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link LogFileWebEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties("management.endpoint.logfile")
public class LogFileWebEndpointProperties {

	/**
	 * External Logfile to be accessed. Can be used if the logfile is written by output
	 * redirect and not by the logging system itself.
	 */
	private @Nullable File externalFile;

	public @Nullable File getExternalFile() {
		return this.externalFile;
	}

	public void setExternalFile(@Nullable File externalFile) {
		this.externalFile = externalFile;
	}

}
