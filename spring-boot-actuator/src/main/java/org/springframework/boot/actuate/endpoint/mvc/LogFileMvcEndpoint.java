/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Controller that provides an API for logfiles, i.e. downloading the main logfile
 * configured in environment property 'logging.file' that is standard, but optional
 * property for spring-boot applications.
 *
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.logfile")
public class LogFileMvcEndpoint extends AbstractMvcEndpoint {

	private static final Log logger = LogFactory.getLog(LogFileMvcEndpoint.class);

	/**
	 * External Logfile to be accessed. Can be used if the logfile is written by output
	 * redirect and not by the logging-system itself.
	 */
	private File externalFile;

	public LogFileMvcEndpoint() {
		super("/logfile", true);
	}

	public File getExternalFile() {
		return this.externalFile;
	}

	public void setExternalFile(File externalFile) {
		this.externalFile = externalFile;
	}

	@RequestMapping(method = { RequestMethod.GET, RequestMethod.HEAD })
	public void invoke(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!isEnabled()) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		Resource resource = getLogFileResource();
		if (resource != null && !resource.exists()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Log file '" + resource + "' does not exist");
			}
			resource = null;
		}
		new Handler(resource).handleRequest(request, response);
	}

	private Resource getLogFileResource() {
		if (this.externalFile != null) {
			return new FileSystemResource(this.externalFile);
		}
		LogFile logFile = LogFile.get(getEnvironment());
		if (logFile == null) {
			logger.debug("Missing 'logging.file' or 'logging.path' properties");
			return null;
		}
		return new FileSystemResource(logFile.toString());
	}

	/**
	 * {@link ResourceHttpRequestHandler} to send the log file.
	 */
	private static class Handler extends ResourceHttpRequestHandler {

		private final Resource resource;

		Handler(Resource resource) {
			this.resource = resource;
			try {
				afterPropertiesSet();
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		protected Resource getResource(HttpServletRequest request) throws IOException {
			return this.resource;
		}

		@Override
		protected MediaType getMediaType(Resource resource) {
			return MediaType.TEXT_PLAIN;
		}

	}

}
