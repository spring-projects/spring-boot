/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.extension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

class MyExtensionSecurityInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String auth = request.getHeader("Authorization");
		if (!"Bearer secret".equals(auth)) {
			response.sendError(HttpStatus.UNAUTHORIZED.value());
			return false;
		}
		return true;
	}

}
