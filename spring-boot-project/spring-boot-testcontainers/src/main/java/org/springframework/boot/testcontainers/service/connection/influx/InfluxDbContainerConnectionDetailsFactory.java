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

package org.springframework.boot.testcontainers.service.connection.influx;

import java.net.URI;

import org.testcontainers.containers.InfluxDBContainer;

import org.springframework.boot.autoconfigure.influx.InfluxDbConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} for
 * {@link InfluxDbServiceConnection @InfluxDbServiceConnection}-annotated
 * {@link InfluxDBContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class InfluxDbContainerConnectionDetailsFactory extends
		ContainerConnectionDetailsFactory<InfluxDbServiceConnection, InfluxDbConnectionDetails, InfluxDBContainer<?>> {

	@Override
	protected InfluxDbConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<InfluxDbServiceConnection, InfluxDbConnectionDetails, InfluxDBContainer<?>> source) {
		return new InfluxDbContainerConnectionDetails(source);
	}

	/**
	 * {@link InfluxDbConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class InfluxDbContainerConnectionDetails extends ContainerConnectionDetails
			implements InfluxDbConnectionDetails {

		private final InfluxDBContainer<?> container;

		private InfluxDbContainerConnectionDetails(
				ContainerConnectionSource<InfluxDbServiceConnection, InfluxDbConnectionDetails, InfluxDBContainer<?>> source) {
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
		public URI getUrl() {
			return URI.create(this.container.getUrl());
		}

	}

}
