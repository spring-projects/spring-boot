/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Strategy interface for customizing {@link WebServerFactory web server factories}. Any
 * beans of this type will get a callback with the server factory before the server itself
 * is started, so you can set the port, address, error pages etc.
 * <p>
 * Beware: calls to this interface are usually made from a
 * {@link WebServerFactoryCustomizerBeanPostProcessor} which is a
 * {@link BeanPostProcessor} (so called very early in the ApplicationContext lifecycle).
 * It might be safer to lookup dependencies lazily in the enclosing BeanFactory rather
 * than injecting them with {@code @Autowired}.
 *
 * @param <T> the configurable web server factory
 * @author Phillip Webb
 * @author Dave Syer
 * @author Brian Clozel
 * @since 2.0.0
 * @see WebServerFactoryCustomizerBeanPostProcessor
 */
@FunctionalInterface
public interface WebServerFactoryCustomizer<T extends WebServerFactory> {

	/**
	 * Customize the specified {@link WebServerFactory}.
	 * @param factory the web server factory to customize
	 */
	void customize(T factory);

}
