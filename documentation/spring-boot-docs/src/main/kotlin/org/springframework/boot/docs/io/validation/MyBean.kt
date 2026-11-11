/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.validation

import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated

@Suppress("UNUSED_PARAMETER")
@Service
@Validated
class MyBean {

	fun findByCodeAndAuthor(code: @Size(min = 8, max = 10) String?, author: Author?): Archive? {
		return null
	}

}

