/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.ldap.test.autoconfigure;

import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.stereotype.Service;

/**
 * Example service used with {@link DataLdapTest @DataLdapTest} tests.
 *
 * @author Eddú Meléndez
 */
@Service
public class ExampleService {

	private final LdapTemplate ldapTemplate;

	public ExampleService(LdapTemplate ldapTemplate) {
		this.ldapTemplate = ldapTemplate;
	}

	public boolean hasEntry(LdapQuery query) {
		return this.ldapTemplate.find(query, ExampleEntry.class).size() == 1;
	}

}
