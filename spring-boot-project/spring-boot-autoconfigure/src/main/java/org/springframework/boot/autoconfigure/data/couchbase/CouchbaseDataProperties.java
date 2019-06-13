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

package org.springframework.boot.autoconfigure.data.couchbase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.couchbase.core.query.Consistency;

/**
 * Configuration properties for Spring Data Couchbase.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.data.couchbase")
public class CouchbaseDataProperties {

	/**
	 * Automatically create views and indexes. Use the meta-data provided by
	 * "@ViewIndexed", "@N1qlPrimaryIndexed" and "@N1qlSecondaryIndexed".
	 */
	private boolean autoIndex;

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

	public Consistency getConsistency() {
		return this.consistency;
	}

	public void setConsistency(Consistency consistency) {
		this.consistency = consistency;
	}

}
