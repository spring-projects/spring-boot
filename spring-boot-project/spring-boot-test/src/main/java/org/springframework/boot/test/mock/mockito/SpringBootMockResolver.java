/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import org.mockito.plugins.MockResolver;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

/**
 * A {@link MockResolver} for testing Spring Boot applications with Mockito. It resolves
 * mocks by walking the proxy chain until the target or a non-static proxy is found.
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 */
public class SpringBootMockResolver implements MockResolver {

	@Override
	public Object resolve(Object instance) {
		return getUltimateTargetObject(instance);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getUltimateTargetObject(Object candidate) {
		Assert.notNull(candidate, "Candidate must not be null");
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised advised) {
				TargetSource targetSource = advised.getTargetSource();
				if (targetSource.isStatic()) {
					Object target = targetSource.getTarget();
					if (target != null) {
						return getUltimateTargetObject(target);
					}
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
		return (T) candidate;
	}

}
