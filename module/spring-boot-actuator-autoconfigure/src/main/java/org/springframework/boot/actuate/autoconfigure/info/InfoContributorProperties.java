/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.info;

import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for core info contributors.
 *
 * @author Stephane Nicoll
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
		 * Mode to use to expose git information.
		 */
		private GitInfoContributor.Mode mode = GitInfoContributor.Mode.SIMPLE;

		public GitInfoContributor.Mode getMode() {
			return this.mode;
		}

		public void setMode(GitInfoContributor.Mode mode) {
			this.mode = mode;
		}

	}

}
