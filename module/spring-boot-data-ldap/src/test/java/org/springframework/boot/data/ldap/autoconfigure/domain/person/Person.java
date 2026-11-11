/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.ldap.autoconfigure.domain.person;

import javax.naming.Name;

import org.jspecify.annotations.Nullable;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.DnAttribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

@Entry(objectClasses = { "person", "top" }, base = "ou=someOu")
public class Person {

	@Id
	private @Nullable Name dn;

	@Attribute(name = "cn")
	@DnAttribute(value = "cn", index = 1)
	private @Nullable String fullName;

}
