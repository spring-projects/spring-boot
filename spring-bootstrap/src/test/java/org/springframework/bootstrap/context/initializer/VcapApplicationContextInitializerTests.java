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

package org.springframework.bootstrap.context.initializer;

import org.junit.Test;
import org.springframework.bootstrap.TestUtils;
import org.springframework.bootstrap.context.initializer.VcapApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link VcapApplicationContextInitializer}.
 * 
 * @author Dave Syer
 */
public class VcapApplicationContextInitializerTests {

	private VcapApplicationContextInitializer initializer = new VcapApplicationContextInitializer();

	@Test
	public void testApplicationProperties() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestUtils
				.addEnviroment(
						context,
						"VCAP_APPLICATION:{\"application_users\":[],\"instance_id\":\"bb7935245adf3e650dfb7c58a06e9ece\",\"instance_index\":0,\"version\":\"3464e092-1c13-462e-a47c-807c30318a50\",\"name\":\"foo\",\"uris\":[\"foo.cfapps.io\"],\"started_at\":\"2013-05-29 02:37:59 +0000\",\"started_at_timestamp\":1369795079,\"host\":\"0.0.0.0\",\"port\":61034,\"limits\":{\"mem\":128,\"disk\":1024,\"fds\":16384},\"version\":\"3464e092-1c13-462e-a47c-807c30318a50\",\"name\":\"dsyerenv\",\"uris\":[\"dsyerenv.cfapps.io\"],\"users\":[],\"start\":\"2013-05-29 02:37:59 +0000\",\"state_timestamp\":1369795079}");
		this.initializer.initialize(context);
		assertEquals("bb7935245adf3e650dfb7c58a06e9ece", context.getEnvironment()
				.getProperty("vcap.application.instance_id"));
	}

	@Test
	public void testServiceProperties() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestUtils
				.addEnviroment(
						context,
						"VCAP_SERVICES:{\"rds-mysql-n/a\":[{\"name\":\"mysql\",\"label\":\"rds-mysql-n/a\",\"plan\":\"10mb\",\"credentials\":{\"name\":\"d04fb13d27d964c62b267bbba1cffb9da\",\"hostname\":\"mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com\",\"host\":\"mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com\",\"port\":3306,\"user\":\"urpRuqTf8Cpe6\",\"username\":\"urpRuqTf8Cpe6\",\"password\":\"pxLsGVpsC9A5S\"}}]}");
		this.initializer.initialize(context);
		assertEquals("mysql",
				context.getEnvironment().getProperty("vcap.services.mysql.name"));
	}
}
