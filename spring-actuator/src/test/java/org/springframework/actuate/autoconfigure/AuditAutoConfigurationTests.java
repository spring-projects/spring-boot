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

package org.springframework.actuate.autoconfigure;

import org.junit.Test;
import org.springframework.actuate.audit.AuditEventRepository;
import org.springframework.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.actuate.security.AuthenticationAuditListener;
import org.springframework.actuate.security.AuthorizationAuditListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link AuditAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class AuditAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testTraceConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(AuditAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(AuditEventRepository.class));
		assertNotNull(this.context.getBean(AuthenticationAuditListener.class));
		assertNotNull(this.context.getBean(AuthorizationAuditListener.class));
	}

	@Test
	public void ownAutoRepository() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(Config.class, AuditAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(AuditEventRepository.class),
				instanceOf(TestAuditEventRepository.class));
	}

	@Configuration
	public static class Config {

		@Bean
		public TestAuditEventRepository testAuditEventRepository() {
			return new TestAuditEventRepository();
		}

	}

	public static class TestAuditEventRepository extends InMemoryAuditEventRepository {
	}

}
