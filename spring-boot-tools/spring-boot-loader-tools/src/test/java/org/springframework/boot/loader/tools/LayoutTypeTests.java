/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.loader.tools;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
public class LayoutTypeTests {

	@Test
	public void standardType() {
		assertThat(LayoutType.layout("DIR"))
				.isEqualTo(LayoutType.valueOf("DIR").layout());
	}

	@Test
	public void customType() {
		assertThat(LayoutType.layout("CUSTOM")).isNotNull();
	}

	public static class TestLayoutFactory implements LayoutFactory {

		@Override
		public Layout getLayout() {
			return new Layouts.Jar();
		}

		@Override
		public String getName() {
			return "CUSTOM";
		}
	}

}
