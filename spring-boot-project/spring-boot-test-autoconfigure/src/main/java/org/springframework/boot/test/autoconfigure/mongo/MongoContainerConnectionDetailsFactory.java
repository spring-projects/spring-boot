/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.mongo;

import com.mongodb.ConnectionString;
import org.testcontainers.containers.MongoDBContainer;

import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} for
 * {@link MongoServiceConnection @MongoServiceConnection}-annotated
 * {@link MongoDBContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class MongoContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<MongoServiceConnection, MongoConnectionDetails, MongoDBContainer> {

	@Override
	protected MongoConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<MongoServiceConnection, MongoConnectionDetails, MongoDBContainer> source) {
		return new MongoContainerConnectionDetails(source);
	}

	/**
	 * {@link MongoConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class MongoContainerConnectionDetails extends ContainerConnectionDetails
			implements MongoConnectionDetails {

		private final ConnectionString connectionString;

		private MongoContainerConnectionDetails(
				ContainerConnectionSource<MongoServiceConnection, MongoConnectionDetails, MongoDBContainer> source) {
			super(source);
			this.connectionString = new ConnectionString(source.getContainer().getReplicaSetUrl());
		}

		@Override
		public ConnectionString getConnectionString() {
			return this.connectionString;
		}

	}

}
