/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.validation;

import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MyBean {

	public Archive findByCodeAndAuthor(@Size(min = 8, max = 10) String code, Author author) {
		return /**/ null;
	}

}
