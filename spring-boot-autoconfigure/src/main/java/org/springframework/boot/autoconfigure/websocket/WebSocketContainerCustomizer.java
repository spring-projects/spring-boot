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

package org.springframework.boot.autoconfigure.websocket;

import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

/**
 * {@link ServletWebServerFactoryCustomizer} to configure websockets for a given
 * {@link ServletWebServerFactory}.
 *
 * @param <T> the {@link ServletWebServerFactory}
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public abstract class WebSocketContainerCustomizer<T extends ServletWebServerFactory>
		implements ServletWebServerFactoryCustomizer, Ordered {

	@Override
	public int getOrder() {
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void customize(ConfigurableServletWebServerFactory factory) {
		if (getWebServerFactoryType().isAssignableFrom(factory.getClass())) {
			doCustomize((T) factory);
		}
	}

	protected Class<?> getWebServerFactoryType() {
		return ResolvableType.forClass(WebSocketContainerCustomizer.class, getClass())
				.resolveGeneric();
	}

	protected abstract void doCustomize(T factory);

}
