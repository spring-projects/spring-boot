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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.session.data.redis.RedisFlushMode;

/**
 * Configuration properties for Redis backed Spring Session.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.session.redis")
public class RedisSessionProperties {

	private static final String DEFAULT_CLEANUP_CRON = "0 * * * * *";

	/**
	 * Namespace for keys used to store sessions.
	 */
	private String namespace = "spring:session";

	/**
	 * Sessions flush mode.
	 */
	private RedisFlushMode flushMode = RedisFlushMode.ON_SAVE;

	/**
	 * Cron expression for expired session cleanup job.
	 */
	private String cleanupCron = DEFAULT_CLEANUP_CRON;

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public RedisFlushMode getFlushMode() {
		return this.flushMode;
	}

	public void setFlushMode(RedisFlushMode flushMode) {
		this.flushMode = flushMode;
	}

	public String getCleanupCron() {
		return this.cleanupCron;
	}

	public void setCleanupCron(String cleanupCron) {
		this.cleanupCron = cleanupCron;
	}

}
