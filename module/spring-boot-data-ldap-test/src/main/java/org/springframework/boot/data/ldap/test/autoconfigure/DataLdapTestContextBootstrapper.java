/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.ldap.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataLdapTest @DataLdapTest} support.
 *
 * @author Artsiom Yudovin
 */
class DataLdapTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataLdapTest> {

}
