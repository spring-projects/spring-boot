package org.springframework.zero.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.zero.SpringApplication;
import org.springframework.zero.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;

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
public abstract class SpringServletInitializer implements WebApplicationInitializer {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		WebApplicationContext rootAppContext = this
				.createRootApplicationContext(servletContext);
		if (rootAppContext != null) {
			servletContext.addListener(new ContextLoaderListener(rootAppContext) {
				@Override
				public void contextInitialized(ServletContextEvent event) {
					// no-op because the application context is already initialized
				}
			});
		} else {
			this.logger
					.debug("No ContextLoaderListener registered, as "
							+ "createRootApplicationContext() did not return an application context");
		}
	}

	protected WebApplicationContext createRootApplicationContext(
			ServletContext servletContext) {
		ApplicationContext parent = null;
		Object object = servletContext
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (object != null && object instanceof ApplicationContext) {
			this.logger.info("Root context already created (using as parent).");
			parent = (ApplicationContext) object;
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
		}
		SpringApplication application = new SpringApplication(
				(Object[]) getConfigClasses());
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.setParent(parent);
		context.setServletContext(servletContext);
		application.setApplicationContext(context);
		return (WebApplicationContext) application.run();
	}

	protected abstract Class<?>[] getConfigClasses();

}
