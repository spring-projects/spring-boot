/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.netty;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyProperties}
 *
 * @author Brian Clozel
 */
class NettyPropertiesTests {

	@Test
	void defaultValueShouldBeConsistent() {
		ResourceLeakDetector.Level defaultLevel = (Level) ReflectionTestUtils.getField(ResourceLeakDetector.class,
				"DEFAULT_LEVEL");
		assertThat(defaultLevel).isEqualTo(Level.SIMPLE);
	}

}
