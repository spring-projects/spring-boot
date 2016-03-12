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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.couchbase.core.query.Consistency;

/**
 * Configuration properties for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.data.couchbase")
public class CouchbaseProperties {

	/**
	 * Automatically create views and indexes. Use the meta-data provided by
	 * "@ViewIndexed", "@N1qlPrimaryIndexed" and "@N1qlSecondaryIndexed".
	 */
	private boolean autoIndex;

	/**
	 * Couchbase nodes (host or IP address) to bootstrap from.
	 */
	private List<String> bootstrapHosts = new ArrayList<String>(
			Collections.singletonList("localhost"));

	private final Bucket bucket = new Bucket();

	/**
	 * Consistency to apply by default on generated queries.
	 */
	private Consistency consistency = Consistency.READ_YOUR_OWN_WRITES;

	public boolean isAutoIndex() {
		return this.autoIndex;
	}

	public void setAutoIndex(boolean autoIndex) {
		this.autoIndex = autoIndex;
	}

	public List<String> getBootstrapHosts() {
		return this.bootstrapHosts;
	}

	public void setBootstrapHosts(List<String> bootstrapHosts) {
		this.bootstrapHosts = bootstrapHosts;
	}

	public Bucket getBucket() {
		return this.bucket;
	}

	public Consistency getConsistency() {
		return this.consistency;
	}

	public void setConsistency(Consistency consistency) {
		this.consistency = consistency;
	}

	static class Bucket {

		/**
		 * Name of the bucket to connect to.
		 */
		private String name;

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
