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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesPulsarConnectionDetails}.
 *
 * @author Chris Bono
 */
class PropertiesPulsarConnectionDetailsTests {

	@Test
	void getClientServiceUrlReturnsValueFromProperties() {
		PulsarProperties properties = new PulsarProperties();
		properties.getClient().setServiceUrl("foo");
		PulsarConnectionDetails connectionDetails = new PropertiesPulsarConnectionDetails(properties);
		assertThat(connectionDetails.getBrokerUrl()).isEqualTo("foo");
	}

	@Test
	void getAdminServiceHttpUrlReturnsValueFromProperties() {
		PulsarProperties properties = new PulsarProperties();
		properties.getAdmin().setServiceUrl("foo");
		PulsarConnectionDetails connectionDetails = new PropertiesPulsarConnectionDetails(properties);
		assertThat(connectionDetails.getAdminUrl()).isEqualTo("foo");
	}

}
