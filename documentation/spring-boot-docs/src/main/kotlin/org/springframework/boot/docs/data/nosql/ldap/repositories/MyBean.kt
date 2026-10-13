/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.ldap.repositories

import org.springframework.ldap.core.LdapTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val template: LdapTemplate) {

	// @fold:on // ...
	fun someMethod(): List<User> {
		return template.findAll(User::class.java)
	}
	// @fold:off

}

