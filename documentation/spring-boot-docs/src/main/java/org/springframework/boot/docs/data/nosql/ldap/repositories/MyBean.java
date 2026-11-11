/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.ldap.repositories;

import java.util.List;

import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final LdapTemplate template;

	public MyBean(LdapTemplate template) {
		this.template = template;
	}

	// @fold:on // ...
	public List<User> someMethod() {
		return this.template.findAll(User.class);
	}
	// @fold:off

}
