/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.boot.autoconfigure.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * AuthenticationEntryPoint that sends a 401 and Parameterized by the value of the
 * WWW-Authenticate header. Like the {@link BasicAuthenticationEntryPoint} but more
 * flexible.
 * 
 * @author Dave Syer
 *
 */
public class Http401AuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final String authenticateHeader;

	public Http401AuthenticationEntryPoint(String authenticateHeader) {
		this.authenticateHeader = authenticateHeader;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		response.setHeader("WWW-Authenticate", authenticateHeader);
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				authException.getMessage());
	}
}