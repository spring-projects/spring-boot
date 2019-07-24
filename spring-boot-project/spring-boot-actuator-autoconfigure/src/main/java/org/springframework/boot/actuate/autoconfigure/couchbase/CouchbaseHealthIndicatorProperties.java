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

package org.springframework.boot.actuate.autoconfigure.couchbase;

import java.time.Duration;

import org.springframework.boot.actuate.couchbase.CouchbaseHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link CouchbaseHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @since 2.0.5
 * @deprecated since 2.0.6
 */
@Deprecated
@ConfigurationProperties(prefix = "management.health.couchbase")
public class CouchbaseHealthIndicatorProperties {

	/**
	 * Timeout for getting the Bucket information from the server.
	 */
	private Duration timeout = Duration.ofMillis(1000);

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

}
