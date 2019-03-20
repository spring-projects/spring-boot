/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.health;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.ldap.LdapDataAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.LdapTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LdapHealthIndicator}
 *
 * @author Eddú Meléndez
 */
public class LdapHealthIndicatorTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void indicatorExists() {
		this.context.register(LdapAutoConfiguration.class,
				LdapDataAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EndpointAutoConfiguration.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		LdapTemplate ldapTemplate = this.context.getBean(LdapTemplate.class);
		assertThat(ldapTemplate).isNotNull();
		LdapHealthIndicator healthIndicator = this.context
				.getBean(LdapHealthIndicator.class);
		assertThat(healthIndicator).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ldapIsUp() {
		LdapTemplate ldapTemplate = mock(LdapTemplate.class);
		given(ldapTemplate.executeReadOnly((ContextExecutor<String>) any()))
				.willReturn("3");
		LdapHealthIndicator healthIndicator = new LdapHealthIndicator(ldapTemplate);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("version")).isEqualTo("3");
		verify(ldapTemplate).executeReadOnly((ContextExecutor<String>) any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ldapIsDown() {
		LdapTemplate ldapTemplate = mock(LdapTemplate.class);
		given(ldapTemplate.executeReadOnly((ContextExecutor<String>) any()))
				.willThrow(new CommunicationException(
						new javax.naming.CommunicationException("Connection failed")));
		LdapHealthIndicator healthIndicator = new LdapHealthIndicator(ldapTemplate);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error"))
				.contains("Connection failed");
		verify(ldapTemplate).executeReadOnly((ContextExecutor<String>) any());
	}

}
