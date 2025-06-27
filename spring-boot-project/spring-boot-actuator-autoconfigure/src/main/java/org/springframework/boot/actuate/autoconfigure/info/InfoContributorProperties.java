/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.info;

import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for core info contributors.
 *
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @since 2.0.0
 */
@ConfigurationProperties("management.info")
public class InfoContributorProperties {

	private final Git git = new Git();

	public Git getGit() {
		return this.git;
	}

	public static class Git {

		/**
		 * Whether to enable git info.
		 */
		private boolean enabled = true;

		/**
		 * Mode to use to expose git information.
		 */
		private GitInfoContributor.Mode mode = GitInfoContributor.Mode.SIMPLE;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public GitInfoContributor.Mode getMode() {
			return this.mode;
		}

		public void setMode(GitInfoContributor.Mode mode) {
			this.mode = mode;
		}

	}

}
