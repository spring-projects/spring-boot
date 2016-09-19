/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@link OncePerRequestFilter} to add a {@literal X-Application-Context} header that
 * contains the {@link ApplicationContext#getId() ApplicationContext ID}.
 *
 * @author Phillip Webb
 * @author Venil Noronha
 * @since 1.4.0
 */
public class ApplicationContextHeaderFilter extends OncePerRequestFilter {

	/**
	 * Public constant for {@literal X-Application-Context}.
	 */
	public static final String HEADER_NAME = "X-Application-Context";

	private final ApplicationContext applicationContext;

	public ApplicationContextHeaderFilter(ApplicationContext context) {
		this.applicationContext = context;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
		response.addHeader(HEADER_NAME, this.applicationContext.getId());
		filterChain.doFilter(request, response);
	}

}
