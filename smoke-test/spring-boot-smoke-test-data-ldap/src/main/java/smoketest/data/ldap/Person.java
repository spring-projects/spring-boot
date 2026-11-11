/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.ldap;

import javax.naming.Name;

import org.jspecify.annotations.Nullable;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

@Entry(objectClasses = { "person", "top" })
public class Person {

	@Id
	private @Nullable Name dn;

	@Attribute(name = "telephoneNumber")
	private @Nullable String phone;

	@Override
	public String toString() {
		return String.format("Customer[dn=%s, phone='%s']", this.dn, this.phone);
	}

}
