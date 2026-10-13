/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.autoconfiguredspringdatajpa.withoutdb

interface UserRepository {

	fun findByUsername(username: String?): User?

}

