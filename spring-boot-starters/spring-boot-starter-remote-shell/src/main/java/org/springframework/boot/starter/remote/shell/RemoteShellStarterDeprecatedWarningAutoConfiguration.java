/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.starter.remote.shell;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to print a deprecation warning about
 * the starter.
 *
 * @author Stephane Nicoll
 * @since 1.5.0
 */
@Configuration
@Deprecated
public class RemoteShellStarterDeprecatedWarningAutoConfiguration {

	private static final Log logger = LogFactory
			.getLog(RemoteShellStarterDeprecatedWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		logger.warn("spring-boot-starter-remote-shell is deprecated as of Spring Boot "
				+ "1.5 and will be removed in Spring Boot 2.0");
	}

}
