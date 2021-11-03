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
import org.springframework.security.core.context.ListeningSecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextChangedListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

public class SecurityContextChangedListenerRegistrar implements InitializingBean, DisposableBean {

	private static final SecurityContextHolderStrategy ORIGINAL_STRATEGY = SecurityContextHolder
			.getContextHolderStrategy();

	private final List<SecurityContextChangedListener> listeners;

	public SecurityContextChangedListenerRegistrar(
			List<SecurityContextChangedListener> securityContextChangedListeners) {
		this.listeners = securityContextChangedListeners;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		SecurityContextHolder.setContextHolderStrategy(
				new ListeningSecurityContextHolderStrategy(ORIGINAL_STRATEGY, this.listeners));
	}

	@Override
	public void destroy() throws Exception {
		SecurityContextHolder.setContextHolderStrategy(ORIGINAL_STRATEGY);
	}

}
