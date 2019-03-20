/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.info;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for project information.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.info")
public class ProjectInfoProperties {

	private final Build build = new Build();

	private final Git git = new Git();

	public Build getBuild() {
		return this.build;
	}

	public Git getGit() {
		return this.git;
	}

	/**
	 * Make sure that the "spring.git.properties" legacy key is used by default.
	 * @param defaultGitLocation the default git location to use
	 */
	@Autowired
	void setDefaultGitLocation(
			@Value("${spring.git.properties:classpath:git.properties}") Resource defaultGitLocation) {
		getGit().setLocation(defaultGitLocation);
	}

	/**
	 * Build specific info properties.
	 */
	public static class Build {

		/**
		 * Location of the generated build-info.properties file.
		 */
		private Resource location = new ClassPathResource(
				"META-INF/build-info.properties");

		public Resource getLocation() {
			return this.location;
		}

		public void setLocation(Resource location) {
			this.location = location;
		}

	}

	/**
	 * Git specific info properties.
	 */
	public static class Git {

		/**
		 * Location of the generated git.properties file.
		 */
		private Resource location;

		public Resource getLocation() {
			return this.location;
		}

		public void setLocation(Resource location) {
			this.location = location;
		}

	}

}
