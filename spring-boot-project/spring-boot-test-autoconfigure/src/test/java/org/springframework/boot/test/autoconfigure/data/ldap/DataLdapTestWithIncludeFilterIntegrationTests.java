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

package org.springframework.boot.test.autoconfigure.data.ldap;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test with custom include filter for {@link DataLdapTest}.
 *
 * @author Eddú Meléndez
 */
@RunWith(SpringRunner.class)
@DataLdapTest(includeFilters = @Filter(Service.class))
@TestPropertySource(properties = { "spring.ldap.embedded.base-dn=dc=spring,dc=org",
		"spring.ldap.embedded.ldif=classpath:org/springframework/boot/test/autoconfigure/data/ldap/schema.ldif" })
public class DataLdapTestWithIncludeFilterIntegrationTests {

	@Autowired
	private ExampleService service;

	@Test
	public void testService() {
		LdapQuery ldapQuery = LdapQueryBuilder.query().where("cn").is("Will Smith");
		assertThat(this.service.hasEntry(ldapQuery)).isFalse();
	}

}
