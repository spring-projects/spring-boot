/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context.testcomponents.listener;

import java.io.IOException;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class TestListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		sce.getServletContext()
			.addFilter("listenerAddedFilter", new ListenerAddedFilter())
			.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
		sce.getServletContext().setAttribute("listenerAttribute", "alpha");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

	}

	static class ListenerAddedFilter implements Filter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			request.setAttribute("listenerAddedFilterAttribute", "charlie");
			chain.doFilter(request, response);
		}

	}

}
