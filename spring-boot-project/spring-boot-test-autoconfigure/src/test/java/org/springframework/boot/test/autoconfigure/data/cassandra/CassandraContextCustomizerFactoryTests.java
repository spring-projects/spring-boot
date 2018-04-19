/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.cassandra;

import org.junit.Test;

import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

public class CassandraContextCustomizerFactoryTests {


	private CassandraContextCustomizerFactory factory = new CassandraContextCustomizerFactory();

	@Test
	public void shouldNotGetContextCustomizer() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(WithAnnotation.class, null);
		assertThat(customizer).isNull();
	}

	@Test
	public void shouldNotGetContextCustomizerForMergedAnnotation() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(WithAnnotationParentAnnotation.class, null);
		assertThat(customizer).isNull();
	}

	@Test
	public void shouldGetContextCustomizer() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(NoAnnotation.class, null);
		assertThat(customizer).isNotNull();
	}


	@DataCassandraTest
	static class WithAnnotationParentAnnotation {

	}

	@AutoConfigureDataCassandra
	static class WithAnnotation {

	}

	static class NoAnnotation {
	}
}
