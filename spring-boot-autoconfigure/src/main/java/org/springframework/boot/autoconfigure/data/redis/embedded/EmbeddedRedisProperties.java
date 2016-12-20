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

package org.springframework.boot.autoconfigure.data.redis.embedded;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Embedded Redis.
 *
 * @author Alexey Zhokhov
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.redis.embedded")
public class EmbeddedRedisProperties {

	/**
	 * Version of Redis to use.
	 */
	private String version = "3.2.1";

	private final Storage storage = new Storage();

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Storage getStorage() {
		return this.storage;
	}

	public static class Storage {

		/**
		 * Directory used for data storage.
		 */
		private String databaseDir;

		public String getDatabaseDir() {
			return this.databaseDir;
		}

		public void setDatabaseDir(String databaseDir) {
			this.databaseDir = databaseDir;
		}

	}

}
