/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.listener;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationEnvironmentAvailableEvent;
import org.springframework.boot.SpringBootTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link VcapApplicationListener}.
 * 
 * @author Dave Syer
 */
public class VcapApplicationListenerTests {

	private VcapApplicationListener initializer = new VcapApplicationListener();
	private ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
	private SpringApplicationEnvironmentAvailableEvent event = new SpringApplicationEnvironmentAvailableEvent(
			new SpringApplication(), this.context.getEnvironment(), new String[0]);

	@Test
	public void testApplicationProperties() {
		SpringBootTestUtils
				.addEnviroment(
						this.context,
						"VCAP_APPLICATION:{\"application_users\":[],\"instance_id\":\"bb7935245adf3e650dfb7c58a06e9ece\",\"instance_index\":0,\"version\":\"3464e092-1c13-462e-a47c-807c30318a50\",\"name\":\"foo\",\"uris\":[\"foo.cfapps.io\"],\"started_at\":\"2013-05-29 02:37:59 +0000\",\"started_at_timestamp\":1369795079,\"host\":\"0.0.0.0\",\"port\":61034,\"limits\":{\"mem\":128,\"disk\":1024,\"fds\":16384},\"version\":\"3464e092-1c13-462e-a47c-807c30318a50\",\"name\":\"dsyerenv\",\"uris\":[\"dsyerenv.cfapps.io\"],\"users\":[],\"start\":\"2013-05-29 02:37:59 +0000\",\"state_timestamp\":1369795079}");
		this.initializer.onApplicationEvent(this.event);
		assertEquals("bb7935245adf3e650dfb7c58a06e9ece", this.context.getEnvironment()
				.getProperty("vcap.application.instance_id"));
	}

	@Test
	public void testUnparseableApplicationProperties() {
		SpringBootTestUtils.addEnviroment(this.context, "VCAP_APPLICATION:");
		this.initializer.onApplicationEvent(this.event);
		assertEquals(null, this.context.getEnvironment().getProperty("vcap"));
	}

	@Test
	public void testNullApplicationProperties() {
		SpringBootTestUtils
				.addEnviroment(
						this.context,
						"VCAP_APPLICATION:{\"application_users\":null,\"instance_id\":\"bb7935245adf3e650dfb7c58a06e9ece\",\"instance_index\":0,\"version\":\"3464e092-1c13-462e-a47c-807c30318a50\",\"name\":\"foo\",\"uris\":[\"foo.cfapps.io\"],\"started_at\":\"2013-05-29 02:37:59 +0000\",\"started_at_timestamp\":1369795079,\"host\":\"0.0.0.0\",\"port\":61034,\"limits\":{\"mem\":128,\"disk\":1024,\"fds\":16384},\"version\":\"3464e092-1c13-462e-a47c-807c30318a50\",\"name\":\"dsyerenv\",\"uris\":[\"dsyerenv.cfapps.io\"],\"users\":[],\"start\":\"2013-05-29 02:37:59 +0000\",\"state_timestamp\":1369795079}");
		this.initializer.onApplicationEvent(this.event);
		assertEquals(null, this.context.getEnvironment().getProperty("vcap"));
	}

	@Test
	public void testServiceProperties() {
		SpringBootTestUtils
				.addEnviroment(
						this.context,
						"VCAP_SERVICES:{\"rds-mysql-n/a\":[{\"name\":\"mysql\",\"label\":\"rds-mysql-n/a\",\"plan\":\"10mb\",\"credentials\":{\"name\":\"d04fb13d27d964c62b267bbba1cffb9da\",\"hostname\":\"mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com\",\"host\":\"mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com\",\"port\":3306,\"user\":\"urpRuqTf8Cpe6\",\"username\":\"urpRuqTf8Cpe6\",\"password\":\"pxLsGVpsC9A5S\"}}]}");
		this.initializer.onApplicationEvent(this.event);
		assertEquals("mysql",
				this.context.getEnvironment().getProperty("vcap.services.mysql.name"));
	}
}
