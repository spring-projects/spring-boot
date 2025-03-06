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

package org.springframework.boot.autoconfigure.pulsar;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to a Pulsar service.
 *
 * @author Chris Bono
 * @since 3.2.0
 */
public interface PulsarConnectionDetails extends ConnectionDetails {

	/**
	 * URL used to connect to the broker.
	 * @return the service URL
	 */
	String getBrokerUrl();

	/**
	 * URL user to connect to the admin endpoint.
	 * @return the admin URL
	 */
	String getAdminUrl();

}
