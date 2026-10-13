/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.server.WebServerFactory;

/**
 * Exception thrown when there is no {@link WebServerFactory} bean of the required type
 * defined in a {@link WebServerApplicationContext}.
 *
 * @author Guirong Hu
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class MissingWebServerFactoryBeanException extends NoSuchBeanDefinitionException {

	private final WebApplicationType webApplicationType;

	/**
	 * Create a new {@code MissingWebServerFactoryBeanException}.
	 * @param webServerApplicationContextClass the class of the
	 * WebServerApplicationContext that required the WebServerFactory
	 * @param webServerFactoryClass the class of the WebServerFactory that was missing
	 * @param webApplicationType the type of the web application
	 */
	public MissingWebServerFactoryBeanException(
			Class<? extends WebServerApplicationContext> webServerApplicationContextClass,
			Class<? extends WebServerFactory> webServerFactoryClass, WebApplicationType webApplicationType) {
		super(webServerFactoryClass, String.format("Unable to start %s due to missing %s bean",
				webServerApplicationContextClass.getSimpleName(), webServerFactoryClass.getSimpleName()));
		this.webApplicationType = webApplicationType;
	}

	/**
	 * Returns the type of web application for which a {@link WebServerFactory} bean was
	 * missing.
	 * @return the type of web application
	 */
	public WebApplicationType getWebApplicationType() {
		return this.webApplicationType;
	}

}
