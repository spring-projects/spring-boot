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

package org.springframework.boot.testcontainers.service.connection.couchbase;

import org.testcontainers.couchbase.CouchbaseContainer;

import org.springframework.boot.autoconfigure.couchbase.CouchbaseConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} for
 * {@link CouchbaseServiceConnection @CouchbaseServiceConnection}-annotated
 * {@link CouchbaseContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class CouchbaseContainerConnectionDetailsFactory extends
		ContainerConnectionDetailsFactory<CouchbaseServiceConnection, CouchbaseConnectionDetails, CouchbaseContainer> {

	@Override
	protected CouchbaseConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<CouchbaseServiceConnection, CouchbaseConnectionDetails, CouchbaseContainer> source) {
		return new CouchbaseContainerConnectionDetails(source);
	}

	/**
	 * {@link CouchbaseConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class CouchbaseContainerConnectionDetails extends ContainerConnectionDetails
			implements CouchbaseConnectionDetails {

		private final CouchbaseContainer container;

		private CouchbaseContainerConnectionDetails(
				ContainerConnectionSource<CouchbaseServiceConnection, CouchbaseConnectionDetails, CouchbaseContainer> source) {
			super(source);
			this.container = source.getContainer();
		}

		@Override
		public String getUsername() {
			return this.container.getUsername();
		}

		@Override
		public String getPassword() {
			return this.container.getPassword();
		}

		@Override
		public String getConnectionString() {
			return this.container.getConnectionString();
		}

	}

}
