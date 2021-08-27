/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.context.SecurityContextChangedListener;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextChangedListenerRegistrar implements InitializingBean, DisposableBean {

	private final List<SecurityContextChangedListener> securityContextChangedListeners;

	public SecurityContextChangedListenerRegistrar(
			List<SecurityContextChangedListener> securityContextChangedListeners) {
		this.securityContextChangedListeners = securityContextChangedListeners;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.securityContextChangedListeners.forEach(SecurityContextHolder::addListener);
	}

	@Override
	public void destroy() throws Exception {
		// this.securityContextChangedListeners.forEach(SecurityContextHolder::removeListener);
	}

}
