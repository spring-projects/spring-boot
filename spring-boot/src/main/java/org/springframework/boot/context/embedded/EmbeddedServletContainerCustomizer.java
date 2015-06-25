/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.context.embedded;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Strategy interface for customizing auto-configured embedded servlet containers. Any
 * beans of this type will get a callback with the container factory before the container
 * itself is started, so you can set the port, address, error pages etc.
 * <p>
 * Beware: calls to this interface are usually made from a
 * {@link EmbeddedServletContainerCustomizerBeanPostProcessor} which is a
 * {@link BeanPostProcessor} (so called very early in the ApplicationContext lifecycle).
 * It might be safer to lookup dependencies lazily in the enclosing BeanFactory rather
 * than injecting them with {@code @Autowired}.
 *
 * @author Dave Syer
 * @see EmbeddedServletContainerCustomizerBeanPostProcessor
 */
public interface EmbeddedServletContainerCustomizer {

	/**
	 * Customize the specified {@link ConfigurableEmbeddedServletContainer}.
	 * @param container the container to customize
	 */
	void customize(ConfigurableEmbeddedServletContainer container);

}
