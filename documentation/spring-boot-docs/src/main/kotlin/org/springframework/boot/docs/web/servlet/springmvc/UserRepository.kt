/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.springmvc

import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Long>
