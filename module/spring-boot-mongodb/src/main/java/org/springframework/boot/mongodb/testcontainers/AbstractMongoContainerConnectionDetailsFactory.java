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

package org.springframework.boot.mongodb.testcontainers;

import java.util.function.Function;

import com.mongodb.ConnectionString;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Abstract {@link ContainerConnectionDetailsFactory} to create
 * {@link MongoConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated MongoDB container.
 *
 * @param <T> type of MongoDB container supported by the factory
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Wouter Blancquaert
 */
abstract class AbstractMongoContainerConnectionDetailsFactory<T extends GenericContainer<T>>
		extends ContainerConnectionDetailsFactory<T, MongoConnectionDetails> {

	private final Function<T, String> connectionStringFunction;

	AbstractMongoContainerConnectionDetailsFactory(Function<T, String> connectionStringFunction) {
		super(ANY_CONNECTION_NAME, "com.mongodb.ConnectionString");
		this.connectionStringFunction = connectionStringFunction;
	}

	@Override
	protected MongoConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<T> source) {
		return new MongoContainerConnectionDetails<>(source, this.connectionStringFunction);
	}

	/**
	 * {@link MongoConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class MongoContainerConnectionDetails<T extends GenericContainer<T>>
			extends ContainerConnectionDetails<T> implements MongoConnectionDetails {

		private final Function<T, String> connectionStringFunction;

		private MongoContainerConnectionDetails(ContainerConnectionSource<T> source,
				Function<T, String> connectionStringFunction) {
			super(source);
			this.connectionStringFunction = connectionStringFunction;
		}

		@Override
		public ConnectionString getConnectionString() {
			return new ConnectionString(this.connectionStringFunction.apply(getContainer()));
		}

		@Override
		public SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
