/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.info;

import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;

/**
 * Information related to Spring projects.
 *
 * @author Jonatan Ivanov
 * @since 3.3.0
 */
public class SpringInfo {

	private final SpringFrameworkInfo framework;

	private final SpringBootInfo boot;

	private final String[] profiles;

	public SpringInfo(Environment environment) {
		this.framework = new SpringFrameworkInfo();
		this.boot = new SpringBootInfo();
		this.profiles = ObjectUtils.isEmpty(environment.getActiveProfiles()) ? environment.getDefaultProfiles()
				: environment.getActiveProfiles();
	}

	public SpringFrameworkInfo getFramework() {
		return this.framework;
	}

	public SpringBootInfo getBoot() {
		return this.boot;
	}

	public String[] getProfiles() {
		return this.profiles;
	}

	/**
	 * Information about Spring Framework.
	 */
	public static class SpringFrameworkInfo {

		private final String version;

		public SpringFrameworkInfo() {
			this.version = SpringVersion.getVersion();
		}

		public String getVersion() {
			return this.version;
		}

	}

	/**
	 * Information about Spring Boot.
	 */
	public static class SpringBootInfo {

		private final String version;

		public SpringBootInfo() {
			this.version = SpringBootVersion.getVersion();
		}

		public String getVersion() {
			return this.version;
		}

	}

}
