/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.function.UnaryOperator;

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
 * @author Alexandre Baron
 */
class AutoConfigurationSorterTests {

	private static final String DEFAULT = OrderUnspecified.class.getName();

	private static final String LOWEST = OrderLowest.class.getName();

	private static final String HIGHEST = OrderHighest.class.getName();

	private static final String A = AutoConfigureA.class.getName();

	private static final String A2 = AutoConfigureA2.class.getName();

	private static final String A3 = AutoConfigureA3.class.getName();

	private static final String A_WITH_REPLACED = AutoConfigureAWithReplaced.class.getName();

	private static final String B = AutoConfigureB.class.getName();

	private static final String B2 = AutoConfigureB2.class.getName();

	private static final String B_WITH_REPLACED = AutoConfigureBWithReplaced.class.getName();

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

	private static final UnaryOperator<String> REPLACEMENT_MAPPER = (name) -> name.replace("Deprecated", "");

	private AutoConfigurationSorter sorter;

	private AutoConfigurationMetadata autoConfigurationMetadata = mock(AutoConfigurationMetadata.class);

	@BeforeEach
	void setup() {
		this.sorter = new AutoConfigurationSorter(new SkipCycleMetadataReaderFactory(), this.autoConfigurationMetadata,
				REPLACEMENT_MAPPER);
	}

	@Test
	void byOrderAnnotation() {
		List<String> actual = getInPriorityOrder(LOWEST, HIGHEST, DEFAULT);
		assertThat(actual).containsExactly(HIGHEST, DEFAULT, LOWEST);
	}

	@Test
	void byAutoConfigureAfter() {
		List<String> actual = getInPriorityOrder(A, B, C);
		assertThat(actual).containsExactly(C, B, A);
	}

	@Test
	void byAutoConfigureAfterAliasFor() {
		List<String> actual = getInPriorityOrder(A3, B2, C);
		assertThat(actual).containsExactly(C, B2, A3);
	}

	@Test
	void byAutoConfigureAfterAliasForWithProperties() throws Exception {
		MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A3, B2, C);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata, REPLACEMENT_MAPPER);
		List<String> actual = getInPriorityOrder(A3, B2, C);
		assertThat(actual).containsExactly(C, B2, A3);
	}

	@Test
	void byAutoConfigureAfterWithDeprecated() {
		List<String> actual = getInPriorityOrder(A_WITH_REPLACED, B_WITH_REPLACED, C);
		assertThat(actual).containsExactly(C, B_WITH_REPLACED, A_WITH_REPLACED);
	}

	@Test
	void byAutoConfigureBefore() {
		List<String> actual = getInPriorityOrder(X, Y, Z);
		assertThat(actual).containsExactly(Z, Y, X);
	}

	@Test
	void byAutoConfigureBeforeAliasFor() {
		List<String> actual = getInPriorityOrder(X, Y2, Z2);
		assertThat(actual).containsExactly(Z2, Y2, X);
	}

	@Test
	void byAutoConfigureBeforeAliasForWithProperties() throws Exception {
		MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(X, Y2, Z2);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata, REPLACEMENT_MAPPER);
		List<String> actual = getInPriorityOrder(X, Y2, Z2);
		assertThat(actual).containsExactly(Z2, Y2, X);
	}

	@Test
	void byAutoConfigureAfterDoubles() {
		List<String> actual = getInPriorityOrder(A, B, C, E);
		assertThat(actual).containsExactly(C, E, B, A);
	}

	@Test
	void byAutoConfigureMixedBeforeAndAfter() {
		List<String> actual = getInPriorityOrder(A, B, C, W, X);
		assertThat(actual).containsExactly(C, W, B, A, X);
	}

	@Test
	void byAutoConfigureMixedBeforeAndAfterWithClassNames() {
		List<String> actual = getInPriorityOrder(A2, B, C, W2, X);
		assertThat(actual).containsExactly(C, W2, B, A2, X);
	}

	@Test
	void byAutoConfigureMixedBeforeAndAfterWithDifferentInputOrder() {
		List<String> actual = getInPriorityOrder(W, X, A, B, C);
		assertThat(actual).containsExactly(C, W, B, A, X);
	}

	@Test
	void byAutoConfigureAfterWithMissing() {
		List<String> actual = getInPriorityOrder(A, B);
		assertThat(actual).containsExactly(B, A);
	}

	@Test
	void byAutoConfigureAfterWithCycle() {
		this.sorter = new AutoConfigurationSorter(new CachingMetadataReaderFactory(), this.autoConfigurationMetadata,
				REPLACEMENT_MAPPER);
		assertThatIllegalStateException().isThrownBy(() -> getInPriorityOrder(A, B, C, D))
			.withMessageContaining("AutoConfigure cycle detected");
	}

	@Test
	void usesAnnotationPropertiesWhenPossible() throws Exception {
		MetadataReaderFactory readerFactory = new SkipCycleMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A2, B, C, W2, X);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata, REPLACEMENT_MAPPER);
		List<String> actual = getInPriorityOrder(A2, B, C, W2, X);
		assertThat(actual).containsExactly(C, W2, B, A2, X);
	}

	@Test
	void useAnnotationWithNoDirectLink() throws Exception {
		MetadataReaderFactory readerFactory = new SkipCycleMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A, B, E);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata, REPLACEMENT_MAPPER);
		List<String> actual = getInPriorityOrder(A, E);
		assertThat(actual).containsExactly(E, A);
	}

	@Test
	void useAnnotationWithNoDirectLinkAndCycle() throws Exception {
		MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A, B, D);
		this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata, REPLACEMENT_MAPPER);
		assertThatIllegalStateException().isThrownBy(() -> getInPriorityOrder(D, B))
			.withMessageContaining("AutoConfigure cycle detected");
	}

	@Test // gh-38904
	void byBeforeAnnotationThenOrderAnnotation() {
		String oa = OrderAutoConfigureA.class.getName();
		String oa1 = OrderAutoConfigureASeedR1.class.getName();
		String oa2 = OrderAutoConfigureASeedY2.class.getName();
		String oa3 = OrderAutoConfigureASeedA3.class.getName();
		String oa4 = OrderAutoConfigureAutoConfigureASeedG4.class.getName();
		List<String> actual = getInPriorityOrder(oa4, oa3, oa2, oa1, oa);
		assertThat(actual).containsExactly(oa1, oa2, oa3, oa4, oa);
	}

	private List<String> getInPriorityOrder(String... classNames) {
		return this.sorter.getInPriorityOrder(Arrays.asList(classNames));
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

	@AutoConfigureAfter(AutoConfigureBWithReplaced.class)
	public static class AutoConfigureAWithReplaced {

	}

	@AutoConfigureAfter({ AutoConfigureC.class, AutoConfigureD.class, AutoConfigureE.class })
	static class AutoConfigureB {

	}

	@AutoConfiguration(after = { AutoConfigureC.class })
	static class AutoConfigureB2 {

	}

	@AutoConfigureAfter({ DeprecatedAutoConfigureC.class, AutoConfigureD.class, AutoConfigureE.class })
	public static class AutoConfigureBWithReplaced {

	}

	static class AutoConfigureC {

	}

	// @DeprecatedAutoConfiguration(replacement =
	// "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureC")
	public static class DeprecatedAutoConfigureC {

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

	// @DeprecatedAutoConfiguration(replacement =
	// "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureY")
	public static class DeprecatedAutoConfigureY {

	}

	@AutoConfigureBefore(AutoConfigureY.class)
	static class AutoConfigureZ {

	}

	@AutoConfiguration(before = AutoConfigureY2.class)
	static class AutoConfigureZ2 {

	}

	static class OrderAutoConfigureA {

	}

	// Use seeds in auto-configuration class names to mislead the sort by names done in
	// AutoConfigurationSorter class.
	@AutoConfigureBefore(OrderAutoConfigureA.class)
	@AutoConfigureOrder(1)
	static class OrderAutoConfigureASeedR1 {

	}

	@AutoConfigureBefore(OrderAutoConfigureA.class)
	@AutoConfigureOrder(2)
	static class OrderAutoConfigureASeedY2 {

	}

	@AutoConfigureBefore(OrderAutoConfigureA.class)
	@AutoConfigureOrder(3)
	static class OrderAutoConfigureASeedA3 {

	}

	@AutoConfigureBefore(OrderAutoConfigureA.class)
	@AutoConfigureOrder(4)
	static class OrderAutoConfigureAutoConfigureASeedG4 {

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
