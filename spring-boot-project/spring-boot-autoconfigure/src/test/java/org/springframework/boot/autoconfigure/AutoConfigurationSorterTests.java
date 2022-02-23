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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfigurationSorter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
class AutoConfigurationSorterTests {

	private static final String DEFAULT = OrderUnspecified.class.getName();

	private static final String LOWEST = OrderLowest.class.getName();

	private static final String HIGHEST = OrderHighest.class.getName();

	private static final String A = AutoConfigureA.class.getName();

	private static final String A2 = AutoConfigureA2.class.getName();

	private static final String A3 = AutoConfigureA3.class.getName();

	private static final String B = AutoConfigureB.class.getName();

	private static final String B2 = AutoConfigureB2.class.getName();

	private static final String C = AutoConfigureC.class.getName();

	private static final String D = AutoConfigureD.class.getName();

	private static final String E = AutoConfigureE.class.getName();

	private static final String W = AutoConfigureW.class.getName();

	private static final String W2 = AutoConfigureW2.class.getName();

	private static final String X = AutoConfigureX.class.getName();

	private static final String Y = AutoConfigureY.class.getName();

	private static final String Y2 = AutoConfigureY2.class.getName();

	private static final String Z = AutoConfigureZ.class.getName();

	private static final String Z2 = AutoConfigureZ2.class.getName();

	private AutoConfigurationSorter sorter;

	private AutoConfigurationMetadata autoConfigurationMetadata = mock(AutoConfigurationMetadata.class);

	@BeforeEach
	void setup() {
		this.sorter = new AutoConfigurationSorter(new SkipCycleMetadataReaderFactory(), this.autoConfigurationMetadata);
	}

	@Test
	void byOrderAnnotation() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(LOWEST, HIGHEST, DEFAULT));
		assertThat(actual).containsExactly(HIGHEST, DEFAULT, LOWEST);
	}

	@Test
	void byAutoConfigureAfter() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C));
		assertThat(actual).containsExactly(C, B, A);
	}

	@Test
	void byAutoConfigureAfterAliasFor() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A3, B2, C));
		assertThat(actual).containsExactly(C, B2, A3);
	}

	@Test
	void byAutoConfigureAfterAliasForWithProperties() throws Exception {
		MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A3, B2, C);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata);
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A3, B2, C));
		assertThat(actual).containsExactly(C, B2, A3);
	}

	@Test
	void byAutoConfigureBefore() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(X, Y, Z));
		assertThat(actual).containsExactly(Z, Y, X);
	}

	@Test
	void byAutoConfigureBeforeAliasFor() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(X, Y2, Z2));
		assertThat(actual).containsExactly(Z2, Y2, X);
	}

	@Test
	void byAutoConfigureBeforeAliasForWithProperties() throws Exception {
		MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(X, Y2, Z2);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata);
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(X, Y2, Z2));
		assertThat(actual).containsExactly(Z2, Y2, X);
	}

	@Test
	void byAutoConfigureAfterDoubles() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, E));
		assertThat(actual).containsExactly(C, E, B, A);
	}

	@Test
	void byAutoConfigureMixedBeforeAndAfter() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, W, X));
		assertThat(actual).containsExactly(C, W, B, A, X);
	}

	@Test
	void byAutoConfigureMixedBeforeAndAfterWithClassNames() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A2, B, C, W2, X));
		assertThat(actual).containsExactly(C, W2, B, A2, X);
	}

	@Test
	void byAutoConfigureMixedBeforeAndAfterWithDifferentInputOrder() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(W, X, A, B, C));
		assertThat(actual).containsExactly(C, W, B, A, X);
	}

	@Test
	void byAutoConfigureAfterWithMissing() {
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B));
		assertThat(actual).containsExactly(B, A);
	}

	@Test
	void byAutoConfigureAfterWithCycle() {
		this.sorter = new AutoConfigurationSorter(new CachingMetadataReaderFactory(), this.autoConfigurationMetadata);
		assertThatIllegalStateException().isThrownBy(() -> this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, D)))
				.withMessageContaining("AutoConfigure cycle detected");
	}

	@Test
	void usesAnnotationPropertiesWhenPossible() throws Exception {
		MetadataReaderFactory readerFactory = new SkipCycleMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A2, B, C, W2, X);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata);
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A2, B, C, W2, X));
		assertThat(actual).containsExactly(C, W2, B, A2, X);
	}

	@Test
	void useAnnotationWithNoDirectLink() throws Exception {
		MetadataReaderFactory readerFactory = new SkipCycleMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A, B, E);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata);
		List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, E));
		assertThat(actual).containsExactly(E, A);
	}

	@Test
	void useAnnotationWithNoDirectLinkAndCycle() throws Exception {
		MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A, B, D);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata);
		assertThatIllegalStateException().isThrownBy(() -> this.sorter.getInPriorityOrder(Arrays.asList(D, B)))
				.withMessageContaining("AutoConfigure cycle detected");
	}

	private AutoConfigurationMetadata getAutoConfigurationMetadata(String... classNames) throws Exception {
		Properties properties = new Properties();
		for (String className : classNames) {
			Class<?> type = ClassUtils.forName(className, null);
			properties.put(type.getName(), "");
			AnnotationMetadata annotationMetadata = AnnotationMetadata.introspect(type);
			addAutoConfigureOrder(properties, className, annotationMetadata);
			addAutoConfigureBefore(properties, className, annotationMetadata);
			addAutoConfigureAfter(properties, className, annotationMetadata);
		}
		return AutoConfigurationMetadataLoader.loadMetadata(properties);
	}

	private void addAutoConfigureAfter(Properties properties, String className, AnnotationMetadata annotationMetadata) {
		Map<String, Object> autoConfigureAfter = annotationMetadata
				.getAnnotationAttributes(AutoConfigureAfter.class.getName(), true);
		if (autoConfigureAfter != null) {
			String value = merge((String[]) autoConfigureAfter.get("value"), (String[]) autoConfigureAfter.get("name"));
			if (!value.isEmpty()) {
				properties.put(className + ".AutoConfigureAfter", value);
			}
		}
	}

	private void addAutoConfigureBefore(Properties properties, String className,
			AnnotationMetadata annotationMetadata) {
		Map<String, Object> autoConfigureBefore = annotationMetadata
				.getAnnotationAttributes(AutoConfigureBefore.class.getName(), true);
		if (autoConfigureBefore != null) {
			String value = merge((String[]) autoConfigureBefore.get("value"),
					(String[]) autoConfigureBefore.get("name"));
			if (!value.isEmpty()) {
				properties.put(className + ".AutoConfigureBefore", value);
			}
		}
	}

	private void addAutoConfigureOrder(Properties properties, String className, AnnotationMetadata annotationMetadata) {
		Map<String, Object> autoConfigureOrder = annotationMetadata
				.getAnnotationAttributes(AutoConfigureOrder.class.getName());
		if (autoConfigureOrder != null) {
			Integer order = (Integer) autoConfigureOrder.get("order");
			if (order != null) {
				properties.put(className + ".AutoConfigureOrder", String.valueOf(order));
			}
		}
	}

	private String merge(String[] value, String[] name) {
		Set<String> items = new LinkedHashSet<>();
		Collections.addAll(items, value);
		Collections.addAll(items, name);
		return StringUtils.collectionToCommaDelimitedString(items);
	}

	@AutoConfigureOrder
	static class OrderUnspecified {

	}

	@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
	static class OrderLowest {

	}

	@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
	static class OrderHighest {

	}

	@AutoConfigureAfter(AutoConfigureB.class)
	static class AutoConfigureA {

	}

	@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureB")
	static class AutoConfigureA2 {

	}

	@AutoConfiguration(after = AutoConfigureB2.class)
	static class AutoConfigureA3 {

	}

	@AutoConfigureAfter({ AutoConfigureC.class, AutoConfigureD.class, AutoConfigureE.class })
	static class AutoConfigureB {

	}

	@AutoConfiguration(after = { AutoConfigureC.class })
	static class AutoConfigureB2 {

	}

	static class AutoConfigureC {

	}

	@AutoConfigureAfter(AutoConfigureA.class)
	static class AutoConfigureD {

	}

	static class AutoConfigureE {

	}

	@AutoConfigureBefore(AutoConfigureB.class)
	static class AutoConfigureW {

	}

	@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureB")
	static class AutoConfigureW2 {

	}

	static class AutoConfigureX {

	}

	@AutoConfigureBefore(AutoConfigureX.class)
	static class AutoConfigureY {

	}

	@AutoConfiguration(before = AutoConfigureX.class)
	static class AutoConfigureY2 {

	}

	@AutoConfigureBefore(AutoConfigureY.class)
	static class AutoConfigureZ {

	}

	@AutoConfiguration(before = AutoConfigureY2.class)
	static class AutoConfigureZ2 {

	}

	static class SkipCycleMetadataReaderFactory extends CachingMetadataReaderFactory {

		@Override
		public MetadataReader getMetadataReader(String className) throws IOException {
			if (className.equals(D)) {
				throw new IOException();
			}
			return super.getMetadataReader(className);
		}

	}

}
