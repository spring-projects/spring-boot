/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.info;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for project information.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties("spring.info")
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
	 * Build specific info properties.
	 */
	public static class Build {

		/**
		 * Location of the generated build-info.properties file.
		 */
		private Resource location = new ClassPathResource("META-INF/build-info.properties");

		/**
		 * File encoding.
		 */
		private Charset encoding = StandardCharsets.UTF_8;

		public Resource getLocation() {
			return this.location;
		}

		public void setLocation(Resource location) {
			this.location = location;
		}

		public Charset getEncoding() {
			return this.encoding;
		}

		public void setEncoding(Charset encoding) {
			this.encoding = encoding;
		}

	}

	/**
	 * Git specific info properties.
	 */
	public static class Git {

		/**
		 * Location of the generated git.properties file.
		 */
		private Resource location = new ClassPathResource("git.properties");

		/**
		 * File encoding.
		 */
		private Charset encoding = StandardCharsets.UTF_8;

		public Resource getLocation() {
			return this.location;
		}

		public void setLocation(Resource location) {
			this.location = location;
		}

		public Charset getEncoding() {
			return this.encoding;
		}

		public void setEncoding(Charset encoding) {
			this.encoding = encoding;
		}

	}

}
