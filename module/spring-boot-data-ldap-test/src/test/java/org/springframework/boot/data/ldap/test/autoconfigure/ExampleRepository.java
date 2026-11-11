/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.ldap.test.autoconfigure;

import org.springframework.data.ldap.repository.LdapRepository;

/**
 * Example repository used with {@link DataLdapTest @DataLdapTest} tests.
 *
 * @author Eddú Meléndez
 */
interface ExampleRepository extends LdapRepository<ExampleEntry> {

}
