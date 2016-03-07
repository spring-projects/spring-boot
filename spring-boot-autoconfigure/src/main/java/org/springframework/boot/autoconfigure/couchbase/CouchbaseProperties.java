/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.couchbase")
public class CouchbaseProperties {

	/**
	 * Couchbase nodes (host or IP address) to bootstrap from.
	 */
	private List<String> bootstrapHosts;

	private final Bucket bucket = new Bucket();

	public List<String> getBootstrapHosts() {
		return this.bootstrapHosts;
	}

	public void setBootstrapHosts(List<String> bootstrapHosts) {
		this.bootstrapHosts = bootstrapHosts;
	}

	public Bucket getBucket() {
		return this.bucket;
	}

	static class Bucket {

		/**
		 * Name of the bucket to connect to.
		 */
		private String name = "default";

		/**
		 * Password of the bucket.
		 */
		private String password = "";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

}
