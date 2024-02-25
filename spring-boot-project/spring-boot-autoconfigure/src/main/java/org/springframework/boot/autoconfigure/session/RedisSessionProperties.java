/*
 * Copyright 2012-2022 the original author or authors.
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
	 * Cron expression for expired session cleanup job. Only supported when
	 * repository-type is set to indexed.
	 */
	private String cleanupCron;

	/**
	 * Type of Redis session repository to configure.
	 */
	private RepositoryType repositoryType = RepositoryType.DEFAULT;

	/**
	 * Returns the namespace of the Redis session.
	 * @return the namespace of the Redis session
	 */
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * Sets the namespace for the Redis session.
	 * @param namespace the namespace to be set
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Returns the flush mode of the RedisSessionProperties.
	 * @return the flush mode of the RedisSessionProperties
	 */
	public FlushMode getFlushMode() {
		return this.flushMode;
	}

	/**
	 * Sets the flush mode for the Redis session.
	 * @param flushMode the flush mode to be set
	 */
	public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	/**
	 * Returns the save mode of the Redis session properties.
	 * @return the save mode of the Redis session properties
	 */
	public SaveMode getSaveMode() {
		return this.saveMode;
	}

	/**
	 * Sets the save mode for Redis session properties.
	 * @param saveMode the save mode to be set
	 */
	public void setSaveMode(SaveMode saveMode) {
		this.saveMode = saveMode;
	}

	/**
	 * Returns the cron expression used for session cleanup.
	 * @return the cron expression used for session cleanup
	 */
	public String getCleanupCron() {
		return this.cleanupCron;
	}

	/**
	 * Sets the cron expression for the cleanup task.
	 * @param cleanupCron the cron expression for the cleanup task
	 */
	public void setCleanupCron(String cleanupCron) {
		this.cleanupCron = cleanupCron;
	}

	/**
	 * Returns the configure action associated with this RedisSessionProperties object.
	 * @return the configure action
	 */
	public ConfigureAction getConfigureAction() {
		return this.configureAction;
	}

	/**
	 * Sets the configure action for RedisSessionProperties.
	 * @param configureAction the configure action to be set
	 */
	public void setConfigureAction(ConfigureAction configureAction) {
		this.configureAction = configureAction;
	}

	/**
	 * Returns the repository type of the RedisSessionProperties.
	 * @return the repository type of the RedisSessionProperties
	 */
	public RepositoryType getRepositoryType() {
		return this.repositoryType;
	}

	/**
	 * Sets the repository type for RedisSessionProperties.
	 * @param repositoryType the repository type to be set
	 */
	public void setRepositoryType(RepositoryType repositoryType) {
		this.repositoryType = repositoryType;
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

	/**
	 * Type of Redis session repository to auto-configure.
	 */
	public enum RepositoryType {

		/**
		 * Auto-configure a RedisSessionRepository.
		 */
		DEFAULT,

		/**
		 * Auto-configure a RedisIndexedSessionRepository.
		 */
		INDEXED

	}

}
