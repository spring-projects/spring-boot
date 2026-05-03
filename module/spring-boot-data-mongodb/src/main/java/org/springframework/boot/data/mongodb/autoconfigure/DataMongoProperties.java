/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.mongodb.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.BigDecimalRepresentation;

/**
 * Configuration properties for Spring Data MongoDB.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.mongodb")
public class DataMongoProperties {

	/**
	 * Whether to enable auto-index creation.
	 */
	private @Nullable Boolean autoIndexCreation;

	/**
	 * Fully qualified name of the FieldNamingStrategy to use.
	 */
	private @Nullable Class<?> fieldNamingStrategy;

	private final Gridfs gridfs = new Gridfs();

	private final Representation representation = new Representation();

	public @Nullable Boolean isAutoIndexCreation() {
		return this.autoIndexCreation;
	}

	public void setAutoIndexCreation(@Nullable Boolean autoIndexCreation) {
		this.autoIndexCreation = autoIndexCreation;
	}

	public @Nullable Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	public void setFieldNamingStrategy(@Nullable Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public Gridfs getGridfs() {
		return this.gridfs;
	}

	public Representation getRepresentation() {
		return this.representation;
	}

	public static class Gridfs {

		/**
		 * GridFS database name.
		 */
		private @Nullable String database;

		/**
		 * GridFS bucket name.
		 */
		private @Nullable String bucket;

		public @Nullable String getDatabase() {
			return this.database;
		}

		public void setDatabase(@Nullable String database) {
			this.database = database;
		}

		public @Nullable String getBucket() {
			return this.bucket;
		}

		public void setBucket(@Nullable String bucket) {
			this.bucket = bucket;
		}

	}

	public static class Representation {

		/**
		 * Representation to use when converting a BigDecimal.
		 */
		private @Nullable BigDecimalRepresentation bigDecimal = BigDecimalRepresentation.UNSPECIFIED;

		public @Nullable BigDecimalRepresentation getBigDecimal() {
			return this.bigDecimal;
		}

		public void setBigDecimal(@Nullable BigDecimalRepresentation bigDecimal) {
			this.bigDecimal = bigDecimal;
		}

	}

}
