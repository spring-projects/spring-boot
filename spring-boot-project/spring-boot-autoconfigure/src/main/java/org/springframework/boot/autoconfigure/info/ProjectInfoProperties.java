/*
 * Copyright 2012-2019 the original author or authors.
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
@ConfigurationProperties(prefix = "spring.info")
public class ProjectInfoProperties {

	private final Build build = new Build();

	private final Git git = new Git();

	/**
     * Returns the build object associated with this ProjectInfoProperties instance.
     *
     * @return the build object
     */
    public Build getBuild() {
		return this.build;
	}

	/**
     * Returns the Git object associated with this ProjectInfoProperties instance.
     *
     * @return the Git object
     */
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

		/**
         * Returns the location resource.
         *
         * @return the location resource
         */
        public Resource getLocation() {
			return this.location;
		}

		/**
         * Sets the location of the resource.
         * 
         * @param location the resource location to be set
         */
        public void setLocation(Resource location) {
			this.location = location;
		}

		/**
         * Returns the encoding used by the Build class.
         * 
         * @return the encoding used by the Build class
         */
        public Charset getEncoding() {
			return this.encoding;
		}

		/**
         * Sets the encoding for the Build class.
         * 
         * @param encoding the encoding to be set
         */
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

		/**
         * Returns the location resource.
         *
         * @return the location resource
         */
        public Resource getLocation() {
			return this.location;
		}

		/**
         * Sets the location of the resource.
         * 
         * @param location the resource location to be set
         */
        public void setLocation(Resource location) {
			this.location = location;
		}

		/**
         * Returns the encoding used by the Git class.
         * 
         * @return the encoding used by the Git class
         */
        public Charset getEncoding() {
			return this.encoding;
		}

		/**
         * Sets the encoding for the Git class.
         * 
         * @param encoding the encoding to be set
         */
        public void setEncoding(Charset encoding) {
			this.encoding = encoding;
		}

	}

}
