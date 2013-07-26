/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.autoconfigure.AutoConfigurationSorter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link AutoConfigurationSorter}.
 * 
 * @author Phillip Webb
 */
public class AutoConfigurationSorterTest {

	private static final String LOWEST = OrderLowest.class.getName();
	private static final String HIGHEST = OrderHighest.class.getName();
	private static final String A = AutoConfigureA.class.getName();
	private static final String B = AutoConfigureB.class.getName();
	private static final String C = AutoConfigureC.class.getName();
	private static final String D = AutoConfigureD.class.getName();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AutoConfigurationSorter sorter;

	@Before
	public void setup() {
		this.sorter = new AutoConfigurationSorter(new DefaultResourceLoader());
	}

	@Test
	public void byOrderAnnotation() throws Exception {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(LOWEST,
				HIGHEST));
		assertThat(actual, equalTo(Arrays.asList(HIGHEST, LOWEST)));
	}

	@Test
	public void byAutoConfigureAfter() throws Exception {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C));
		assertThat(actual, equalTo(Arrays.asList(C, B, A)));
	}

	@Test
	public void byAutoConfigureAfterWithMissing() throws Exception {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B));
		assertThat(actual, equalTo(Arrays.asList(B, A)));
	}

	@Test
	public void byAutoConfigureAfterWithCycle() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Cycle");
		this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, D));
	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	public static class OrderLowest {
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	public static class OrderHighest {
	}

	@AutoConfigureAfter(AutoConfigureB.class)
	public static class AutoConfigureA {
	}

	@AutoConfigureAfter({ AutoConfigureC.class, AutoConfigureD.class })
	public static class AutoConfigureB {
	}

	public static class AutoConfigureC {
	}

	@AutoConfigureAfter(AutoConfigureA.class)
	public static class AutoConfigureD {
	}
}
