/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.hateoas.server.core.DummyInvocationUtils;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HateoasObjenesisCacheDisabler}.
 *
 * @author Andy Wilkinson
 */
public class HateoasObjenesisCacheDisablerTests {

	private ObjenesisStd objenesis;

	@Before
	@After
	public void resetCacheField() {
		ReflectionTestUtils.setField(HateoasObjenesisCacheDisabler.class, "cacheDisabled",
				false);
		this.objenesis = (ObjenesisStd) ReflectionTestUtils
				.getField(DummyInvocationUtils.class, "OBJENESIS");
		ReflectionTestUtils.setField(this.objenesis, "cache",
				new ConcurrentHashMap<String, ObjectInstantiator<?>>());
	}

	@Test
	public void cacheIsEnabledByDefault() {
		assertThat(this.objenesis.getInstantiatorOf(TestObject.class))
				.isSameAs(this.objenesis.getInstantiatorOf(TestObject.class));
	}

	@Test
	public void cacheIsDisabled() {
		new HateoasObjenesisCacheDisabler().afterPropertiesSet();
		assertThat(this.objenesis.getInstantiatorOf(TestObject.class))
				.isNotSameAs(this.objenesis.getInstantiatorOf(TestObject.class));
	}

	private static class TestObject {

	}

}
