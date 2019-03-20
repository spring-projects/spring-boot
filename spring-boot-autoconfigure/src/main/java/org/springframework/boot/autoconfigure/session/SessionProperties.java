/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.hazelcast.HazelcastFlushMode;

/**
 * Configuration properties for Spring Session.
 *
 * @author Tommy Ludwig
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.session")
public class SessionProperties {

	/**
	 * Session store type.
	 */
	private StoreType storeType;

	private final Integer timeout;

	private final Hazelcast hazelcast = new Hazelcast();

	private final Jdbc jdbc = new Jdbc();

	private final Mongo mongo = new Mongo();

	private final Redis redis = new Redis();

	public SessionProperties(ObjectProvider<ServerProperties> serverProperties) {
		ServerProperties properties = serverProperties.getIfUnique();
		this.timeout = (properties != null) ? properties.getSession().getTimeout() : null;
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

		/**
		 * Sessions flush mode.
		 */
		private HazelcastFlushMode flushMode = HazelcastFlushMode.ON_SAVE;

		public String getMapName() {
			return this.mapName;
		}

		public void setMapName(String mapName) {
			this.mapName = mapName;
		}

		public HazelcastFlushMode getFlushMode() {
			return this.flushMode;
		}

		public void setFlushMode(HazelcastFlushMode flushMode) {
			this.flushMode = flushMode;
		}

	}

	public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
				+ "session/jdbc/schema-@@platform@@.sql";

		private static final String DEFAULT_TABLE_NAME = "SPRING_SESSION";

		/**
		 * Path to the SQL file to use to initialize the database schema.
		 */
		private String schema = DEFAULT_SCHEMA_LOCATION;

		/**
		 * Name of database table used to store sessions.
		 */
		private String tableName = DEFAULT_TABLE_NAME;

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

		public class Initializer {

			/**
			 * Create the required session tables on startup if necessary. Enabled
			 * automatically if the default table name is set or a custom schema is
			 * configured.
			 */
			private Boolean enabled;

			public boolean isEnabled() {
				if (this.enabled != null) {
					return this.enabled;
				}
				boolean defaultTableName = DEFAULT_TABLE_NAME
						.equals(Jdbc.this.getTableName());
				boolean customSchema = !DEFAULT_SCHEMA_LOCATION
						.equals(Jdbc.this.getSchema());
				return (defaultTableName || customSchema);
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
		 * Sessions flush mode.
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
