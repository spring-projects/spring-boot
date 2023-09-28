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

package org.springframework.boot.autoconfigure.amqp;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails.Address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PropertiesRabbitConnectionDetailsTest {

	@Test
	void getAddresses() {
		RabbitProperties properties = new RabbitProperties();
		properties.setAddresses("localhost:1234,[::1]:32863");
		PropertiesRabbitConnectionDetails propertiesRabbitConnectionDetails = new PropertiesRabbitConnectionDetails(properties);
		List<Address> addresses = propertiesRabbitConnectionDetails.getAddresses();
		assertThat(addresses.size()).isEqualTo(2);
		assertThat(addresses.get(0).host()).isEqualTo("localhost");
		assertThat(addresses.get(1).host()).isEqualTo("[::1]");
		assertThat(addresses.get(0).port()).isEqualTo(1234);
		assertThat(addresses.get(1).port()).isEqualTo(32863);
	}
}