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

package org.springframework.boot.test.context;

import kotlin.Metadata;
import org.junit.Test;
import org.spockframework.runtime.model.SpecMetadata;
import spock.lang.Issue;
import spock.lang.Stepwise;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImportsContextCustomizer}.
 *
 * @author Andy Wilkinson
 */
public class ImportsContextCustomizerTests {

	@Test
	public void customizersForTestClassesWithDifferentKotlinMetadataAreEqual() {
		assertThat(new ImportsContextCustomizer(FirstKotlinAnnotatedTestClass.class))
				.isEqualTo(new ImportsContextCustomizer(
						SecondKotlinAnnotatedTestClass.class));
	}

	@Test
	public void customizersForTestClassesWithDifferentSpockFrameworkAnnotationsAreEqual() {
		assertThat(
				new ImportsContextCustomizer(FirstSpockFrameworkAnnotatedTestClass.class))
						.isEqualTo(new ImportsContextCustomizer(
								SecondSpockFrameworkAnnotatedTestClass.class));
	}

	@Test
	public void customizersForTestClassesWithDifferentSpockLangAnnotationsAreEqual() {
		assertThat(new ImportsContextCustomizer(FirstSpockLangAnnotatedTestClass.class))
				.isEqualTo(new ImportsContextCustomizer(
						SecondSpockLangAnnotatedTestClass.class));
	}

	@Metadata(d2 = "foo")
	static class FirstKotlinAnnotatedTestClass {

	}

	@Metadata(d2 = "bar")
	static class SecondKotlinAnnotatedTestClass {

	}

	@SpecMetadata(filename = "foo", line = 10)
	static class FirstSpockFrameworkAnnotatedTestClass {

	}

	@SpecMetadata(filename = "bar", line = 10)
	static class SecondSpockFrameworkAnnotatedTestClass {

	}

	@Stepwise
	static class FirstSpockLangAnnotatedTestClass {

	}

	@Issue("1234")
	static class SecondSpockLangAnnotatedTestClass {

	}

}
