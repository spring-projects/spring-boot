/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.bind;

import java.util.Iterator;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RelaxedNames}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class RelaxedNamesTests {

	@Test
	public void iterator() throws Exception {
		Iterator<String> iterator = new RelaxedNames("my-RELAXED-property").iterator();
		assertThat(iterator.next()).isEqualTo("my-RELAXED-property");
		assertThat(iterator.next()).isEqualTo("my_RELAXED_property");
		assertThat(iterator.next()).isEqualTo("myRELAXEDProperty");
		assertThat(iterator.next()).isEqualTo("myRelaxedProperty");
		assertThat(iterator.next()).isEqualTo("my-relaxed-property");
		assertThat(iterator.next()).isEqualTo("my_relaxed_property");
		assertThat(iterator.next()).isEqualTo("myrelaxedproperty");
		assertThat(iterator.next()).isEqualTo("MY-RELAXED-PROPERTY");
		assertThat(iterator.next()).isEqualTo("MY_RELAXED_PROPERTY");
		assertThat(iterator.next()).isEqualTo("MYRELAXEDPROPERTY");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void fromUnderscores() throws Exception {
		Iterator<String> iterator = new RelaxedNames("nes_ted").iterator();
		assertThat(iterator.next()).isEqualTo("nes_ted");
		assertThat(iterator.next()).isEqualTo("nes.ted");
		assertThat(iterator.next()).isEqualTo("nesTed");
		assertThat(iterator.next()).isEqualTo("nested");
		assertThat(iterator.next()).isEqualTo("NES_TED");
		assertThat(iterator.next()).isEqualTo("NES.TED");
		assertThat(iterator.next()).isEqualTo("NESTED");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void fromPlain() throws Exception {
		Iterator<String> iterator = new RelaxedNames("plain").iterator();
		assertThat(iterator.next()).isEqualTo("plain");
		assertThat(iterator.next()).isEqualTo("PLAIN");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void fromCamelCase() throws Exception {
		Iterator<String> iterator = new RelaxedNames("caMel").iterator();
		assertThat(iterator.next()).isEqualTo("caMel");
		assertThat(iterator.next()).isEqualTo("ca_mel");
		assertThat(iterator.next()).isEqualTo("ca-mel");
		assertThat(iterator.next()).isEqualTo("camel");
		assertThat(iterator.next()).isEqualTo("CAMEL");
		assertThat(iterator.next()).isEqualTo("CA_MEL");
		assertThat(iterator.next()).isEqualTo("CA-MEL");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void fromCompoundCamelCase() throws Exception {
		Iterator<String> iterator = new RelaxedNames("caMelCase").iterator();
		assertThat(iterator.next()).isEqualTo("caMelCase");
		assertThat(iterator.next()).isEqualTo("ca_mel_case");
		assertThat(iterator.next()).isEqualTo("ca-mel-case");
		assertThat(iterator.next()).isEqualTo("camelcase");
		assertThat(iterator.next()).isEqualTo("CAMELCASE");
		assertThat(iterator.next()).isEqualTo("CA_MEL_CASE");
		assertThat(iterator.next()).isEqualTo("CA-MEL-CASE");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void fromPeriods() throws Exception {
		Iterator<String> iterator = new RelaxedNames("spring.value").iterator();
		assertThat(iterator.next()).isEqualTo("spring.value");
		assertThat(iterator.next()).isEqualTo("spring_value");
		assertThat(iterator.next()).isEqualTo("springValue");
		assertThat(iterator.next()).isEqualTo("springvalue");
		assertThat(iterator.next()).isEqualTo("SPRING.VALUE");
		assertThat(iterator.next()).isEqualTo("SPRING_VALUE");
		assertThat(iterator.next()).isEqualTo("SPRINGVALUE");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void fromPrefixEndingInPeriod() throws Exception {
		Iterator<String> iterator = new RelaxedNames("spring.").iterator();
		assertThat(iterator.next()).isEqualTo("spring.");
		assertThat(iterator.next()).isEqualTo("spring_");
		assertThat(iterator.next()).isEqualTo("SPRING.");
		assertThat(iterator.next()).isEqualTo("SPRING_");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void fromEmpty() throws Exception {
		Iterator<String> iterator = new RelaxedNames("").iterator();
		assertThat(iterator.next()).isEqualTo("");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void forCamelCase() throws Exception {
		Iterator<String> iterator = RelaxedNames.forCamelCase("camelCase").iterator();
		assertThat(iterator.next()).isEqualTo("camel-case");
		assertThat(iterator.next()).isEqualTo("camel_case");
		assertThat(iterator.next()).isEqualTo("camelCase");
		assertThat(iterator.next()).isEqualTo("camelcase");
		assertThat(iterator.next()).isEqualTo("CAMEL-CASE");
		assertThat(iterator.next()).isEqualTo("CAMEL_CASE");
		assertThat(iterator.next()).isEqualTo("CAMELCASE");
	}

	@Test
	public void forCamelCaseWithCaps() throws Exception {
		Iterator<String> iterator = RelaxedNames.forCamelCase("camelCASE").iterator();
		assertThat(iterator.next()).isEqualTo("camel-c-a-s-e");
		assertThat(iterator.next()).isEqualTo("camel_c_a_s_e");
		assertThat(iterator.next()).isEqualTo("camelCASE");
		assertThat(iterator.next()).isEqualTo("camelcase");
		assertThat(iterator.next()).isEqualTo("CAMEL-C-A-S-E");
		assertThat(iterator.next()).isEqualTo("CAMEL_C_A_S_E");
		assertThat(iterator.next()).isEqualTo("CAMELCASE");
	}

}
