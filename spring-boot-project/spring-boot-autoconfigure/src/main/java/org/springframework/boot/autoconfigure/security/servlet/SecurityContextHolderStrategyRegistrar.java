/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * Registrar for registering {@link SecurityContextHolderStrategy} instances.
 *
 * @author Jonatan Ivanov
 * @since 2.6.0
 */
public class SecurityContextHolderStrategyRegistrar implements DisposableBean {

	private static final SecurityContextHolderStrategy ORIGINAL = SecurityContextHolder.getContextHolderStrategy();

	public SecurityContextHolderStrategyRegistrar(@Nullable SecurityContextHolderStrategy strategy) {
		if (strategy != null) {
			SecurityContextHolder.setContextHolderStrategy(strategy);
		}
	}

	@Override
	public void destroy() throws Exception {
		SecurityContextHolder.setContextHolderStrategy(ORIGINAL);
	}

}
