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
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a MongoDB service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Jay Choi
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
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

	/**
	 * The {@link ReadConcern} for MongoDB.
	 * @return the read concern option
	 */
	default @Nullable ReadConcern getReadConcern() {
		return null;
	}

	/**
	 * The {@link WriteConcern} for MongoDB.
	 * @return the write concern options
	 */
	default @Nullable WriteConcern getWriteConcern() {
		return null;
	}

	/**
	 * The {@link ReadPreference} for MongoDB.
	 * @return the read preference options
	 */
	default @Nullable ReadPreference getReadPreference() {
		return null;
	}

}
