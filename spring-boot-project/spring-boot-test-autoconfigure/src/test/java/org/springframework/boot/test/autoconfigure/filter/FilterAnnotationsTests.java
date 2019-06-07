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

package org.springframework.boot.test.autoconfigure.filter;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FilterAnnotations}.
 *
 * @author Phillip Webb
 */
public class FilterAnnotationsTests {

	@Test
	public void filterAnnotation() throws Exception {
		FilterAnnotations filterAnnotations = get(FilterByAnnotation.class);
		assertThat(match(filterAnnotations, ExampleWithAnnotation.class)).isTrue();
		assertThat(match(filterAnnotations, ExampleWithoutAnnotation.class)).isFalse();
	}

	@Test
	public void filterAssignableType() throws Exception {
		FilterAnnotations filterAnnotations = get(FilterByType.class);
		assertThat(match(filterAnnotations, ExampleWithAnnotation.class)).isFalse();
		assertThat(match(filterAnnotations, ExampleWithoutAnnotation.class)).isTrue();
	}

	@Test
	public void filterCustom() throws Exception {
		FilterAnnotations filterAnnotations = get(FilterByCustom.class);
		assertThat(match(filterAnnotations, ExampleWithAnnotation.class)).isFalse();
		assertThat(match(filterAnnotations, ExampleWithoutAnnotation.class)).isTrue();
	}

	@Test
	public void filterAspectJ() throws Exception {
		FilterAnnotations filterAnnotations = get(FilterByAspectJ.class);
		assertThat(match(filterAnnotations, ExampleWithAnnotation.class)).isFalse();
		assertThat(match(filterAnnotations, ExampleWithoutAnnotation.class)).isTrue();
	}

	@Test
	public void filterRegex() throws Exception {
		FilterAnnotations filterAnnotations = get(FilterByRegex.class);
		assertThat(match(filterAnnotations, ExampleWithAnnotation.class)).isFalse();
		assertThat(match(filterAnnotations, ExampleWithoutAnnotation.class)).isTrue();
	}

	private FilterAnnotations get(Class<?> type) {
		Filters filters = AnnotatedElementUtils.getMergedAnnotation(type, Filters.class);
		return new FilterAnnotations(getClass().getClassLoader(), filters.value());
	}

	private boolean match(FilterAnnotations filterAnnotations, Class<?> type) throws IOException {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(type.getName());
		return filterAnnotations.anyMatches(metadataReader, metadataReaderFactory);
	}

	@Filters(@Filter(Service.class))
	static class FilterByAnnotation {

	}

	@Filters(@Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExampleWithoutAnnotation.class))
	static class FilterByType {

	}

	@Filters(@Filter(type = FilterType.CUSTOM, classes = ExampleCustomFilter.class))
	static class FilterByCustom {

	}

	@Filters(@Filter(type = FilterType.ASPECTJ, pattern = "(*..*ExampleWithoutAnnotation)"))
	static class FilterByAspectJ {

	}

	@Filters(@Filter(type = FilterType.REGEX, pattern = ".*ExampleWithoutAnnotation"))
	static class FilterByRegex {

	}

	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	static @interface Filters {

		Filter[] value();

	}

	static class ExampleCustomFilter implements TypeFilter {

		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
			return metadataReader.getClassMetadata().getClassName().equals(ExampleWithoutAnnotation.class.getName());
		}

	}

	@Service
	static class ExampleWithAnnotation {

	}

	static class ExampleWithoutAnnotation {

	}

}
