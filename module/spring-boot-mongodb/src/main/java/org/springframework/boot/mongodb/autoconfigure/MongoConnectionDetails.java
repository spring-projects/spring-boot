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

package org.springframework.boot.mongodb.autoconfigure;

import com.mongodb.ConnectionString;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a MongoDB service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface MongoConnectionDetails extends ConnectionDetails {

	/**
	 * The {@link ConnectionString} for MongoDB.
	 * @return the connection string
	 */
	ConnectionString getConnectionString();

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use
	 */
	default SslBundle getSslBundle() {
		return null;
	}

	/**
	 * GridFS configuration.
	 * @return the GridFS configuration or {@code null}
	 */
	default GridFs getGridFs() {
		return null;
	}

	/**
	 * GridFS configuration.
	 */
	interface GridFs {

		/**
		 * GridFS database name.
		 * @return the GridFS database name or {@code null}
		 */
		String getDatabase();

		/**
		 * GridFS bucket name.
		 * @return the GridFS bucket name or {@code null}
		 */
		String getBucket();

		/**
		 * Factory method to create a new {@link GridFs} instance.
		 * @param database the database
		 * @param bucket the bucket name
		 * @return a new {@link GridFs} instance
		 */
		static GridFs of(String database, String bucket) {
			return new GridFs() {

				@Override
				public String getDatabase() {
					return database;
				}

				@Override
				public String getBucket() {
					return bucket;
				}

			};
		}

	}

}
