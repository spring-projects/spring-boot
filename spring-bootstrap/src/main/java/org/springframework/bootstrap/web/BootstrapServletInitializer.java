package org.springframework.bootstrap.web;

import javax.servlet.Filter;

import org.springframework.util.ClassUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * <p>
 * A handy opinionated {@link WebApplicationInitializer} for applications that only have
 * one Spring servlet, and no more than a single filter (which itself is only enabled when
 * Spring Security is detected). If your application is more complicated consider using
 * one of the other WebApplicationInitializers.
 * <p>
 * 
 * <p>
 * Note that a WebApplicationInitializer is only needed if you are building a war file and
 * deploying it. If you prefer to run an embedded container (we do) then you won't need
 * this at all.
 * </p>
 * 
 * @author Dave Syer
 * 
 */
public abstract class BootstrapServletInitializer extends
		AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return null;
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}

	@Override
	protected Filter[] getServletFilters() {
		if (ClassUtils.isPresent(
				"org.springframework.security.config.annotation.web.EnableWebSecurity",
				null)) {
			DelegatingFilterProxy filter = new DelegatingFilterProxy(
					"springSecurityFilterChain");
			filter.setContextAttribute(FrameworkServlet.SERVLET_CONTEXT_PREFIX
					+ "dispatcher");
			return new Filter[] { filter };
		}
		return new Filter[0];
	}

}
