/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.test.autoconfigure.webmvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.stereotype.Component;

/**
 * {@link WebSecurityCustomizer} used to test their inclusion in
 * {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 */
@Component
class ExampleWebSecurityCustomizer implements WebSecurityCustomizer {

	@Override
	public void customize(WebSecurity web) {
	}

}
