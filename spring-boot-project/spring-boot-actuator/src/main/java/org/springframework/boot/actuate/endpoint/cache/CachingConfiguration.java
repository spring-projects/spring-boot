/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.cache;

/**
 * The caching configuration of an endpoint.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CachingConfiguration {

	private final long timeToLive;

	/**
	 * Create a new instance with the given {@code timeToLive}.
	 * @param timeToLive the time to live of an operation result in milliseconds
	 */
	public CachingConfiguration(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * Returns the time to live of a cached operation result.
	 * @return the time to live of an operation result
	 */
	public long getTimeToLive() {
		return this.timeToLive;
	}

}
