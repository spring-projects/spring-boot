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

package org.springframework.boot.test.mock.mockito;

import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mockito.internal.matchers.LocalizedMatcher;
import org.mockito.internal.progress.ArgumentMatcherStorage;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.verification.MockAwareVerificationMode;
import org.mockito.verification.VerificationMode;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.Assert;

/**
 * AOP {@link Interceptor} that attempts to make AOP proxy beans work with Mockito. Works
 * by bypassing AOP advice when a method is invoked via
 * {@code Mockito#verify(Object) verify(mock)}.
 *
 * @author Phillip Webb
 */
class MockitoAopProxyTargetInterceptor implements MethodInterceptor {

	private final Object source;

	private final Object target;

	private final Verification verification;

	MockitoAopProxyTargetInterceptor(Object source, Object target) throws Exception {
		this.source = source;
		this.target = target;
		this.verification = new Verification(target);
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (this.verification.isVerifying()) {
			this.verification.replaceVerifyMock(this.source, this.target);
			return AopUtils.invokeJoinpointUsingReflection(this.target,
					invocation.getMethod(), invocation.getArguments());
		}
		return invocation.proceed();
	}

	@Autowired
	public static void applyTo(Object source) {
		Assert.state(AopUtils.isAopProxy(source), "Source must be an AOP proxy");
		try {
			Advised advised = (Advised) source;
			for (Advisor advisor : advised.getAdvisors()) {
				if (advisor instanceof MockitoAopProxyTargetInterceptor) {
					return;
				}
			}
			Object target = AopTestUtils.getUltimateTargetObject(source);
			Advice advice = new MockitoAopProxyTargetInterceptor(source, target);
			advised.addAdvice(0, advice);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to apply Mockito AOP support", ex);
		}
	}

	private static class Verification {

		private final Object monitor = new Object();

		private final MockingProgress progress;

		Verification(Object target) {
			this.progress = MockitoApi.get().mockingProgress(target);
		}

		public boolean isVerifying() {
			synchronized (this.monitor) {
				VerificationMode mode = this.progress.pullVerificationMode();
				if (mode != null) {
					resetVerificationStarted(mode);
					return true;
				}
				return false;
			}
		}

		public void replaceVerifyMock(Object source, Object target) {
			synchronized (this.monitor) {
				VerificationMode mode = this.progress.pullVerificationMode();
				if (mode != null) {
					if (mode instanceof MockAwareVerificationMode) {
						MockAwareVerificationMode mockAwareMode = (MockAwareVerificationMode) mode;
						if (mockAwareMode.getMock() == source) {
							mode = MockitoApi.get().createMockAwareVerificationMode(
									target, mockAwareMode);
						}
					}
					resetVerificationStarted(mode);
				}
			}
		}

		private void resetVerificationStarted(VerificationMode mode) {
			ArgumentMatcherStorage storage = this.progress.getArgumentMatcherStorage();
			List<LocalizedMatcher> matchers = storage.pullLocalizedMatchers();
			this.progress.verificationStarted(mode);
			MockitoApi.get().reportMatchers(storage, matchers);
		}

	}

}
