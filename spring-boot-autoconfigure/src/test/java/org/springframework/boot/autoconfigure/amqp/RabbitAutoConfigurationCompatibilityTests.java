/*
 * Copyright 2012-2018 the original author or authors.
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

import javax.net.ssl.TrustManager;

import com.rabbitmq.client.NullTrustManager;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.junit.runner.classpath.ClassPathOverrides;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compatibility tests for RabbitMQ with amqp-client 4.0.x
 *
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathOverrides("com.rabbitmq:amqp-client:4.0.3")
public class RabbitAutoConfigurationCompatibilityTests
		extends RabbitAutoConfigurationTests {

	@Test
	public void enableSslWithValidateServerCertificateFalse() {
		load(TestConfiguration.class, "spring.rabbitmq.ssl.enabled:true",
				"spring.rabbitmq.ssl.validateServerCertificate=false");
		com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory();
		TrustManager trustManager = getTrustManager(rabbitConnectionFactory);
		assertThat(trustManager).isInstanceOf(NullTrustManager.class);
	}

}
