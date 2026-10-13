/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.autoconfiguredspringdataldap.server;

import org.springframework.boot.data.ldap.test.autoconfigure.DataLdapTest;
import org.springframework.boot.ldap.autoconfigure.embedded.EmbeddedLdapAutoConfiguration;

@DataLdapTest(excludeAutoConfiguration = EmbeddedLdapAutoConfiguration.class)
class MyDataLdapTests {

	// ...

}
