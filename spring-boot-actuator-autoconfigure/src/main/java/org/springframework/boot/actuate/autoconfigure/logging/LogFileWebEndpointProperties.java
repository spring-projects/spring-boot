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

package org.springframework.boot.actuate.autoconfigure.logging;

import java.io.File;

import org.springframework.boot.actuate.logger.LogFileWebEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link LogFileWebEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "endpoints.logfile")
public class LogFileWebEndpointProperties {

	/**
	 * External Logfile to be accessed. Can be used if the logfile is written by output
	 * redirect and not by the logging system itself.
	 */
	private File externalFile;

	public File getExternalFile() {
		return this.externalFile;
	}

	public void setExternalFile(File externalFile) {
		this.externalFile = externalFile;
	}

}
