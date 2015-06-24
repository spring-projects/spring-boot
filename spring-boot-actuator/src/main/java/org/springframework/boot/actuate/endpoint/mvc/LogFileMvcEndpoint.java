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

package org.springframework.boot.actuate.endpoint.mvc;

import java.io.IOException;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogFile;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller that provides an API for logfiles, i.e. downloading the main logfile
 * configured in environment property 'logging.file' that is standard, but optional
 * property for spring-boot applications.
 *
 * @author Johannes Stelzer
 * @author Phillip Webb
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.logfile")
public class LogFileMvcEndpoint implements MvcEndpoint, EnvironmentAware {

	private static final Log logger = LogFactory.getLog(LogFileMvcEndpoint.class);

	/**
	 * Endpoint URL path.
	 */
	@NotNull
	@Pattern(regexp = "/[^/]*", message = "Path must start with /")
	private String path = "/logfile";

	/**
	 * Enable security on the endpoint.
	 */

	private boolean sensitive = true;

	/**
	 * Enable the endpoint.
	 */

	private boolean enabled = true;

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Endpoint> getEndpointType() {
		return null;
	}

	@RequestMapping(method = RequestMethod.HEAD)
	@ResponseBody
	public ResponseEntity<?> available() {
		return getResponse(false);
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> invoke() throws IOException {
		return getResponse(true);
	}

	private ResponseEntity<?> getResponse(boolean includeBody) {
		if (!isEnabled()) {
			return (includeBody ? DISABLED_RESPONSE : ResponseEntity.notFound().build());
		}
		Resource resource = getLogFileResource();
		if (resource == null) {
			return ResponseEntity.notFound().build();
		}
		BodyBuilder response = ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN);
		return (includeBody ? response.body(resource) : response.build());
	}

	private Resource getLogFileResource() {
		LogFile logFile = LogFile.get(this.environment);
		if (logFile == null) {
			logger.debug("Missing 'logging.file' or 'logging.path' properties");
			return null;
		}
		FileSystemResource resource = new FileSystemResource(logFile.toString());
		if (!resource.exists()) {
			if (logger.isWarnEnabled()) {
				logger.debug("Log file '" + resource + "' does not exist");
			}
			return null;
		}
		return resource;
	}

}
