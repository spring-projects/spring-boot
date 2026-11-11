/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.test.autoconfigure.webmvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.stereotype.Component;

/**
 * {@link WebSecurityConfigurer} used to test their inclusion in
 * {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 */
@Component
class ExampleWebSecurityConfigurer implements WebSecurityConfigurer<WebSecurity> {

	@Override
	public void init(WebSecurity builder) {
	}

	@Override
	public void configure(WebSecurity builder) {
	}

}
