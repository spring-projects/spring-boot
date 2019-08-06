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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;

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
	 * Sessions flush mode. Determines when session changes are written to the session
	 * store.
	 */
	private FlushMode flushMode = FlushMode.ON_SAVE;

	/**
	 * Sessions save mode. Determines how session changes are tracked and saved to the
	 * session store.
	 */
	private SaveMode saveMode = SaveMode.ON_SET_ATTRIBUTE;

	/**
	 * The configure action to apply when no user defined ConfigureRedisAction bean is
	 * present.
	 */
	private ConfigureAction configureAction = ConfigureAction.NOTIFY_KEYSPACE_EVENTS;

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

	public FlushMode getFlushMode() {
		return this.flushMode;
	}

	public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	public SaveMode getSaveMode() {
		return this.saveMode;
	}

	public void setSaveMode(SaveMode saveMode) {
		this.saveMode = saveMode;
	}

	public String getCleanupCron() {
		return this.cleanupCron;
	}

	public void setCleanupCron(String cleanupCron) {
		this.cleanupCron = cleanupCron;
	}

	public ConfigureAction getConfigureAction() {
		return this.configureAction;
	}

	public void setConfigureAction(ConfigureAction configureAction) {
		this.configureAction = configureAction;
	}

	/**
	 * Strategies for configuring and validating Redis.
	 */
	public enum ConfigureAction {

		/**
		 * Ensure that Redis Keyspace events for Generic commands and Expired events are
		 * enabled.
		 */
		NOTIFY_KEYSPACE_EVENTS,

		/**
		 * No not attempt to apply any custom Redis configuration.
		 */
		NONE

	}

}
