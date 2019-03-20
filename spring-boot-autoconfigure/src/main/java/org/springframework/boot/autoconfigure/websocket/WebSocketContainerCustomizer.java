/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.websocket;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

/**
 * {@link EmbeddedServletContainerCustomizer} to configure websockets for a given
 * {@link EmbeddedServletContainerFactory}.
 *
 * @param <T> the embedded servlet container factory
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public abstract class WebSocketContainerCustomizer<T extends EmbeddedServletContainerFactory>
		implements EmbeddedServletContainerCustomizer, Ordered {

	@Override
	public int getOrder() {
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		if (getContainerType().isAssignableFrom(container.getClass())) {
			doCustomize((T) container);
		}
	}

	protected Class<?> getContainerType() {
		return ResolvableType.forClass(WebSocketContainerCustomizer.class, getClass())
				.resolveGeneric();
	}

	protected abstract void doCustomize(T container);

}
