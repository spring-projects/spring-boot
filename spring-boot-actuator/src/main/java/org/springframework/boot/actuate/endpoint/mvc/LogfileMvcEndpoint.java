/*
 * Copyright 2012-2014 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogFile;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller that provides an API for logfiles, i.e. downloading the main logfile
 * configured in environment property 'logging.file' that is standard, but optional
 * property for spring-boot applications.
 *
 * @author Johannes Stelzer
 */
@ConfigurationProperties(prefix = "endpoints.logfile")
public class LogfileMvcEndpoint implements MvcEndpoint, EnvironmentAware {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(LogfileMvcEndpoint.class);

	private String path = "/logfile";

	private boolean sensitive = true;

	private boolean enabled = true;

	private Environment environment;

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Endpoint> getEndpointType() {
		return null;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> invoke() throws IOException {
		if (!isAvailable()) {
			return ResponseEntity.notFound().build();
		}

		FileSystemResource file = new FileSystemResource(LogFile.get(this.environment)
				.toString());

		return ResponseEntity
				.ok()
				.contentType(MediaType.TEXT_PLAIN)
				.header("Content-Disposition",
						"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

	@RequestMapping(method = RequestMethod.HEAD)
	@ResponseBody
	public ResponseEntity<?> available() {
		if (isAvailable()) {
			return ResponseEntity.ok().build();
		}
		else {
			return ResponseEntity.notFound().build();
		}
	}

	private boolean isAvailable() {
		if (!this.enabled) {
			return false;
		}

		LogFile logFile = LogFile.get(this.environment);
		if (logFile == null) {
			LOGGER.debug("Logfile download failed for missing property 'logging.file'");
			return false;
		}

		if (!new FileSystemResource(logFile.toString()).exists()) {
			LOGGER.error("Logfile download failed for missing file at path={}",
					logFile.toString());
			return false;
		}

		return true;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}
