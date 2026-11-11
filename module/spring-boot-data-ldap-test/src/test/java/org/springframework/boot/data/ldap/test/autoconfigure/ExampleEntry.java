/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.ldap.test.autoconfigure;

import javax.naming.Name;

import org.jspecify.annotations.Nullable;

import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

/**
 * Example entry used with {@link DataLdapTest @DataLdapTest} tests.
 *
 * @author Eddú Meléndez
 */
@Entry(objectClasses = { "person", "top" })
public class ExampleEntry {

	@Id
	private @Nullable Name dn;

	public @Nullable Name getDn() {
		return this.dn;
	}

	public void setDn(@Nullable Name dn) {
		this.dn = dn;
	}

}
