/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.junit.Test;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.boot.actuate.security.AbstractAuthorizationAuditListener;
import org.springframework.boot.actuate.security.AuthenticationAuditListener;
import org.springframework.boot.actuate.security.AuthorizationAuditListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.event.AbstractAuthorizationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link AuditAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 */
public class AuditAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testTraceConfiguration() throws Exception {
		registerAndRefresh(AuditAutoConfiguration.class);
		assertNotNull(this.context.getBean(AuditEventRepository.class));
		assertNotNull(this.context.getBean(AuthenticationAuditListener.class));
		assertNotNull(this.context.getBean(AuthorizationAuditListener.class));
	}

	@Test
	public void ownAutoRepository() throws Exception {
		registerAndRefresh(CustomAuditEventRepositoryConfiguration.class,
				AuditAutoConfiguration.class);
		assertThat(this.context.getBean(AuditEventRepository.class),
				instanceOf(TestAuditEventRepository.class));
	}

	@Test
	public void ownAuthenticationAuditListener() throws Exception {
		registerAndRefresh(CustomAuthenticationAuditListenerConfiguration.class,
				AuditAutoConfiguration.class);
		assertThat(this.context.getBean(AbstractAuthenticationAuditListener.class),
				instanceOf(TestAuthenticationAuditListener.class));
	}

	@Test
	public void ownAuthorizationAuditListener() throws Exception {
		registerAndRefresh(CustomAuthorizationAuditListenerConfiguration.class,
				AuditAutoConfiguration.class);
		assertThat(this.context.getBean(AbstractAuthorizationAuditListener.class),
				instanceOf(TestAuthorizationAuditListener.class));
	}

	private void registerAndRefresh(Class<?>... annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
	}

	@Configuration
	public static class CustomAuditEventRepositoryConfiguration {

		@Bean
		public TestAuditEventRepository testAuditEventRepository() {
			return new TestAuditEventRepository();
		}

	}

	public static class TestAuditEventRepository extends InMemoryAuditEventRepository {
	}

	@Configuration
	protected static class CustomAuthenticationAuditListenerConfiguration {

		@Bean
		public TestAuthenticationAuditListener authenticationAuditListener() {
			return new TestAuthenticationAuditListener();
		}

	}

	protected static class TestAuthenticationAuditListener
			extends AbstractAuthenticationAuditListener {

		@Override
		public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		}

		@Override
		public void onApplicationEvent(AbstractAuthenticationEvent event) {
		}

	}

	@Configuration
	protected static class CustomAuthorizationAuditListenerConfiguration {

		@Bean
		public TestAuthorizationAuditListener authorizationAuditListener() {
			return new TestAuthorizationAuditListener();
		}

	}

	protected static class TestAuthorizationAuditListener
			extends AbstractAuthorizationAuditListener {

		@Override
		public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		}

		@Override
		public void onApplicationEvent(AbstractAuthorizationEvent event) {
		}

	}

}
