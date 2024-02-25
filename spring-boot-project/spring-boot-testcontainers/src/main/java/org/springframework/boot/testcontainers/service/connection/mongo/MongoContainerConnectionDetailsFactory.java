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

package org.springframework.boot.testcontainers.service.connection.mongo;

import com.mongodb.ConnectionString;
import org.testcontainers.containers.MongoDBContainer;

import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link MongoConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link MongoDBContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class MongoContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<MongoDBContainer, MongoConnectionDetails> {

	/**
     * Constructs a new MongoContainerConnectionDetailsFactory object.
     * 
     * @param connectionName the name of the connection
     * @param connectionString the connection string
     */
    MongoContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "com.mongodb.ConnectionString");
	}

	/**
     * Returns the connection details for the specified container connection source.
     * 
     * @param source the container connection source
     * @return the connection details for the container
     */
    @Override
	protected MongoConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<MongoDBContainer> source) {
		return new MongoContainerConnectionDetails(source);
	}

	/**
	 * {@link MongoConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class MongoContainerConnectionDetails extends ContainerConnectionDetails<MongoDBContainer>
			implements MongoConnectionDetails {

		/**
         * Constructs a new MongoContainerConnectionDetails object with the specified ContainerConnectionSource.
         * 
         * @param source the ContainerConnectionSource used to create the MongoDB container connection
         */
        private MongoContainerConnectionDetails(ContainerConnectionSource<MongoDBContainer> source) {
			super(source);
		}

		/**
         * Returns the connection string for the MongoDB container.
         * 
         * @return the connection string
         */
        @Override
		public ConnectionString getConnectionString() {
			return new ConnectionString(getContainer().getReplicaSetUrl());
		}

	}

}
