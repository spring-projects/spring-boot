/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.servlet.server;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Strategy interface for customizing auto-configured web server factory. Any beans of
 * this type will get a callback with the server factory before the server itself is
 * started, so you can set the port, address, error pages etc.
 * <p>
 * Beware: calls to this interface are usually made from a
 * {@link ServletWebServerFactoryCustomizerBeanPostProcessor} which is a
 * {@link BeanPostProcessor} (so called very early in the ApplicationContext lifecycle).
 * It might be safer to lookup dependencies lazily in the enclosing BeanFactory rather
 * than injecting them with {@code @Autowired}.
 *
 * @author Dave Syer
 * @since 2.0.0
 * @see ServletWebServerFactoryCustomizerBeanPostProcessor
 */
@FunctionalInterface
public interface ServletWebServerFactoryCustomizer {

	/**
	 * Customize the specified {@link ConfigurableServletWebServerFactory}.
	 * @param serverFactory the server factory to customize
	 */
	void customize(ConfigurableServletWebServerFactory serverFactory);

}
