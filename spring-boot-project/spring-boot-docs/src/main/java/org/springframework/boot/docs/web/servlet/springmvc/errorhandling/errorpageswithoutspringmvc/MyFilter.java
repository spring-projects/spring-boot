/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.web.servlet.springmvc.errorhandling.errorpageswithoutspringmvc;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.web.filter.GenericFilterBean;

/**
 * MyFilter class.
 */
class MyFilter extends GenericFilterBean {

	/**
     * This method is used to filter incoming requests and responses in a servlet application.
     * It implements the doFilter method from the Filter interface.
     *
     * @param request  the ServletRequest object representing the incoming request
     * @param response the ServletResponse object representing the outgoing response
     * @param chain    the FilterChain object used to invoke the next filter in the chain
     * @throws IOException      if an I/O error occurs during the filtering process
     * @throws ServletException if a servlet-specific error occurs during the filtering process
     */
    @Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
	}

}
