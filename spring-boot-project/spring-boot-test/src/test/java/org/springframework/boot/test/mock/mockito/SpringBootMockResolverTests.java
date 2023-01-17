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

import org.junit.jupiter.api.Test;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.aop.target.SingletonTargetSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootMockResolver}.
 *
 * @author Moritz Halbritter
 */
class SpringBootMockResolverTests {

	@Test
	void testStaticTarget() {
		MyServiceImpl myService = new MyServiceImpl();
		MyService proxy = ProxyFactory.getProxy(MyService.class, new SingletonTargetSource(myService));
		Object target = new SpringBootMockResolver().resolve(proxy);
		assertThat(target).isInstanceOf(MyServiceImpl.class);
	}

	@Test
	void testNonStaticTarget() {
		MyServiceImpl myService = new MyServiceImpl();
		MyService proxy = ProxyFactory.getProxy(MyService.class, new HotSwappableTargetSource(myService));
		Object target = new SpringBootMockResolver().resolve(proxy);
		assertThat(target).isInstanceOf(SpringProxy.class);
	}

	private interface MyService {

		int a();

	}

	private static class MyServiceImpl implements MyService {

		@Override
		public int a() {
			return 1;
		}

	}

}
