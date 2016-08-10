/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.hornetq;

import org.hornetq.spi.core.naming.BindingRegistry;

/**
 * A no-op implementation of the {@link org.hornetq.spi.core.naming.BindingRegistry}.
 *
 * @author Stephane Nicoll
 * @since 1.1.0
 * @deprecated as of 1.4 in favor of the artemis support
 */
@Deprecated
public class HornetQNoOpBindingRegistry implements BindingRegistry {

	@Override
	public Object lookup(String name) {
		// This callback is used to check if an entry is present in the context before
		// creating a queue on the fly. This is actually never used to try to fetch a
		// destination that is unknown.
		return null;
	}

	@Override
	public boolean bind(String name, Object obj) {
		// This callback is used bind a Destination created on the fly by the embedded
		// broker using the JNDI name that was specified in the configuration. This does
		// not look very useful since it's used nowhere. It could be interesting to
		// autowire a destination to use it but the wiring is a bit "asynchronous" so
		// better not provide that feature at all.
		return false;
	}

	@Override
	public void unbind(String name) {
	}

	@Override
	public void close() {
	}

	@Override
	public Object getContext() {
		return this;
	}

	@Override
	public void setContext(Object ctx) {
	}

}
