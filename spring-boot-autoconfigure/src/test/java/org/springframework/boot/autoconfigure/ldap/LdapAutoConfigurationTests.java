/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.ldap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.ldap.core.ContextSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LdapAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
public class LdapAutoConfigurationTests {

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
	public void testDefaultUrl() {
		load();
		assertThat(this.context.getBeanNamesForType(ContextSource.class).length)
				.isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(this.context.getBean(ContextSource.class),
				"urls")).isEqualTo(new String[]{"ldap://localhost:389"});
	}

	@Test
	public void testContextSourceSetOneUrl() {
		load("spring.ldap.urls:ldap://localhost:123");
		assertThat(this.context.getBeanNamesForType(ContextSource.class).length)
				.isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(this.context.getBean(ContextSource.class),
				"urls")).isEqualTo(new String[]{"ldap://localhost:123"});
	}

	@Test
	public void testContextSourceSetTwoUrls() {
		load("spring.ldap.urls:ldap://localhost:123,ldap://mycompany:123");
		assertThat(this.context.getBeanNamesForType(ContextSource.class).length)
				.isEqualTo(1);
		assertThat(this.context.getBean(LdapProperties.class).getUrls().length)
				.isEqualTo(2);
		assertThat(ReflectionTestUtils.getField(this.context.getBean(ContextSource.class),
				"urls"))
				.isEqualTo(new String[]{"ldap://localhost:123", "ldap://mycompany:123"});
	}

	@Test
	public void testContextSourceWithMoreProperties() {
		load("spring.ldap.urls:ldap://localhost:123",
				"spring.ldap.username:root",
				"spring.ldap.password:root",
				"spring.ldap.base:cn=SpringDevelopers",
				"spring.ldap.baseEnvironment.java.naming.security" +
						".authentication:DIGEST-MD5");
		assertThat(this.context.getBeanNamesForType(ContextSource.class).length)
				.isEqualTo(1);
		assertThat(this.context.getBean(LdapProperties.class).getBaseEnvironment())
				.containsEntry("java.naming.security.authentication", "DIGEST-MD5");
	}

	private void load(String... properties) {
		EnvironmentTestUtils.addEnvironment(this.context, properties);
		this.context
				.register(LdapAutoConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

}
