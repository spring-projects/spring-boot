/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.springmvc.errorhandling.errorpageswithoutspringmvc

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.web.filter.GenericFilterBean

class MyFilter : GenericFilterBean() {

	override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
	}

}

