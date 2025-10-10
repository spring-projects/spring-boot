/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.security.test.autoconfigure.webmvc;

import java.io.IOException;
import java.security.Principal;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.core.Ordered;

/**
 * {@link Filter} that is ordered to run after Spring Security's filter.
 *
 * @author Andy Wilkinson
 */
public class AfterSecurityFilter implements Filter, Ordered {

	@Override
	public int getOrder() {
		return SecurityProperties.DEFAULT_FILTER_ORDER + 1;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Principal principal = ((HttpServletRequest) request).getUserPrincipal();
		if (principal == null) {
			throw new ServletException("No user principal");
		}
		response.getWriter().write(principal.getName());
		response.getWriter().flush();
	}

}
