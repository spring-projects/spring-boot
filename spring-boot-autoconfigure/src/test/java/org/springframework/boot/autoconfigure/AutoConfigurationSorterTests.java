/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.Ordered;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfigurationSorter}.
 *
 * @author Phillip Webb
 */
public class AutoConfigurationSorterTests {

	private static final String LOWEST = OrderLowest.class.getName();

	private static final String HIGHEST = OrderHighest.class.getName();

	private static final String A = AutoConfigureA.class.getName();

	private static final String B = AutoConfigureB.class.getName();

	private static final String C = AutoConfigureC.class.getName();

	private static final String D = AutoConfigureD.class.getName();

	private static final String E = AutoConfigureE.class.getName();

	private static final String W = AutoConfigureW.class.getName();

	private static final String X = AutoConfigureX.class.getName();

	private static final String Y = AutoConfigureY.class.getName();

	private static final String Z = AutoConfigureZ.class.getName();

	private static final String A2 = AutoConfigureA2.class.getName();

	private static final String W2 = AutoConfigureW2.class.getName();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AutoConfigurationSorter sorter;

	private AutoConfigurationMetadata autoConfigurationMetadata = mock(
			AutoConfigurationMetadata.class);

	@Before
	public void setup() {
		this.sorter = new AutoConfigurationSorter(new CachingMetadataReaderFactory(),
				this.autoConfigurationMetadata);
	}

	@Test
	public void byOrderAnnotation() throws Exception {
		List<String> actual = this.sorter
				.getInPriorityOrder(Arrays.asList(LOWEST, HIGHEST));
		assertThat(actual).containsExactly(HIGHEST, LOWEST);
	}

	@Test
	public void byAutoConfigureAfter() throws Exception {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C));
		assertThat(actual).containsExactly(C, B, A);
	}

	@Test
	public void byAutoConfigureBefore() throws Exception {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(X, Y, Z));
		assertThat(actual).containsExactly(Z, Y, X);
	}

	@Test
	public void byAutoConfigureAfterDoubles() throws Exception {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, E));
		assertThat(actual).containsExactly(C, E, B, A);
	}

	@Test
	public void byAutoConfigureMixedBeforeAndAfter() throws Exception {
		List<String> actual = this.sorter
				.getInPriorityOrder(Arrays.asList(A, B, C, W, X));
		assertThat(actual).containsExactly(C, W, B, A, X);
	}

	@Test
	public void byAutoConfigureMixedBeforeAndAfterWithClassNames() throws Exception {
		List<String> actual = this.sorter
				.getInPriorityOrder(Arrays.asList(A2, B, C, W2, X));
		assertThat(actual).containsExactly(C, W2, B, A2, X);
	}

	@Test
	public void byAutoConfigureMixedBeforeAndAfterWithDifferentInputOrder()
			throws Exception {
		List<String> actual = this.sorter
				.getInPriorityOrder(Arrays.asList(W, X, A, B, C));
		assertThat(actual).containsExactly(C, W, B, A, X);
	}

	@Test
	public void byAutoConfigureAfterWithMissing() throws Exception {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B));
		assertThat(actual).containsExactly(B, A);
	}

	@Test
	public void byAutoConfigureAfterWithCycle() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("AutoConfigure cycle detected");
		this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, D));
	}

	@Test
	public void usesAnnotationPropertiesWhenPossible() throws Exception {
		MetadataReaderFactory readerFactory = mock(MetadataReaderFactory.class);
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A2, B, C, W2, X);
		this.sorter = new AutoConfigurationSorter(readerFactory,
				this.autoConfigurationMetadata);
		List<String> actual = this.sorter
				.getInPriorityOrder(Arrays.asList(A2, B, C, W2, X));
		assertThat(actual).containsExactly(C, W2, B, A2, X);
	}

	private AutoConfigurationMetadata getAutoConfigurationMetadata(String... classNames)
			throws Exception {
		Properties properties = new Properties();
		for (String className : classNames) {
			Class<?> type = ClassUtils.forName(className, null);
			properties.put(type.getName(), "");
			AutoConfigureOrder order = type
					.getDeclaredAnnotation(AutoConfigureOrder.class);
			if (order != null) {
				properties.put(className + ".AutoConfigureOrder",
						String.valueOf(order.value()));
			}
			AutoConfigureBefore autoConfigureBefore = type
					.getDeclaredAnnotation(AutoConfigureBefore.class);
			if (autoConfigureBefore != null) {
				properties.put(className + ".AutoConfigureBefore",
						merge(autoConfigureBefore.value(), autoConfigureBefore.name()));
			}
			AutoConfigureAfter autoConfigureAfter = type
					.getDeclaredAnnotation(AutoConfigureAfter.class);
			if (autoConfigureAfter != null) {
				properties.put(className + ".AutoConfigureAfter",
						merge(autoConfigureAfter.value(), autoConfigureAfter.name()));
			}
		}
		return AutoConfigurationMetadataLoader.loadMetadata(properties);
	}

	private String merge(Class<?>[] value, String[] name) {
		Set<String> items = new LinkedHashSet<String>();
		for (Class<?> type : value) {
			items.add(type.getName());
		}
		for (String type : name) {
			items.add(type);
		}
		return StringUtils.collectionToCommaDelimitedString(items);
	}

	@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
	public static class OrderLowest {

	}

	@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
	public static class OrderHighest {

	}

	@AutoConfigureAfter(AutoConfigureB.class)
	public static class AutoConfigureA {

	}

	@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureB")
	public static class AutoConfigureA2 {

	}

	@AutoConfigureAfter({ AutoConfigureC.class, AutoConfigureD.class,
			AutoConfigureE.class })
	public static class AutoConfigureB {

	}

	public static class AutoConfigureC {

	}

	@AutoConfigureAfter(AutoConfigureA.class)
	public static class AutoConfigureD {

	}

	public static class AutoConfigureE {

	}

	@AutoConfigureBefore(AutoConfigureB.class)
	public static class AutoConfigureW {

	}

	@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureB")
	public static class AutoConfigureW2 {

	}

	public static class AutoConfigureX {

	}

	@AutoConfigureBefore(AutoConfigureX.class)
	public static class AutoConfigureY {

	}

	@AutoConfigureBefore(AutoConfigureY.class)
	public static class AutoConfigureZ {

	}

}
