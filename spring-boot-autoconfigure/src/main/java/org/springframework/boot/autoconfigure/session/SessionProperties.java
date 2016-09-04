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

package org.springframework.boot.autoconfigure.session;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.session.data.redis.RedisFlushMode;

/**
 * Configuration properties for Spring Session.
 *
 * @author Tommy Ludwig
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 1.4.0
 */
@ConfigurationProperties("spring.session")
public class SessionProperties {

	/**
	 * Session store type.
	 */
	private StoreType storeType;

	private Integer timeout;

	private final Hazelcast hazelcast = new Hazelcast();

	private final Jdbc jdbc = new Jdbc();

	private final Mongo mongo = new Mongo();

	private final Redis redis = new Redis();

	public SessionProperties(ObjectProvider<ServerProperties> serverProperties) {
		ServerProperties properties = serverProperties.getIfUnique();
		this.timeout = (properties != null ? properties.getSession().getTimeout() : null);
	}

	public StoreType getStoreType() {
		return this.storeType;
	}

	public void setStoreType(StoreType storeType) {
		this.storeType = storeType;
	}

	/**
	 * Return the session timeout in seconds.
	 * @return the session timeout in seconds
	 * @see ServerProperties#getSession()
	 */
	public Integer getTimeout() {
		return this.timeout;
	}

	public Hazelcast getHazelcast() {
		return this.hazelcast;
	}

	public Jdbc getJdbc() {
		return this.jdbc;
	}

	public Mongo getMongo() {
		return this.mongo;
	}

	public Redis getRedis() {
		return this.redis;
	}

	public static class Hazelcast {

		/**
		 * Name of the map used to store sessions.
		 */
		private String mapName = "spring:session:sessions";

		public String getMapName() {
			return this.mapName;
		}

		public void setMapName(String mapName) {
			this.mapName = mapName;
		}

	}

	public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
				+ "session/jdbc/schema-@@platform@@.sql";

		/**
		 * Path to the SQL file to use to initialize the database schema.
		 */
		private String schema = DEFAULT_SCHEMA_LOCATION;

		/**
		 * Name of database table used to store sessions.
		 */
		private String tableName = "SPRING_SESSION";

		private final Initializer initializer = new Initializer();

		public String getSchema() {
			return this.schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public String getTableName() {
			return this.tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public Initializer getInitializer() {
			return this.initializer;
		}

		public static class Initializer {

			/**
			 * Create the required session tables on startup if necessary.
			 */
			private boolean enabled = true;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

	}

	public static class Mongo {

		/**
		 * Collection name used to store sessions.
		 */
		private String collectionName = "sessions";

		public String getCollectionName() {
			return this.collectionName;
		}

		public void setCollectionName(String collectionName) {
			this.collectionName = collectionName;
		}

	}

	public static class Redis {

		/**
		 * Namespace for keys used to store sessions.
		 */
		private String namespace = "";

		/**
		 * Flush mode for the Redis sessions.
		 */
		private RedisFlushMode flushMode = RedisFlushMode.ON_SAVE;

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

	}

}
