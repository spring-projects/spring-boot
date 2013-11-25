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

package org.springframework.boot.bind;

import java.util.Iterator;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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
		assertThat(iterator.next(), equalTo("my-RELAXED-property"));
		assertThat(iterator.next(), equalTo("my_RELAXED_property"));
		assertThat(iterator.next(), equalTo("myRELAXEDProperty"));
		assertThat(iterator.next(), equalTo("my-relaxed-property"));
		assertThat(iterator.next(), equalTo("my_relaxed_property"));
		assertThat(iterator.next(), equalTo("myrelaxedproperty"));
		assertThat(iterator.next(), equalTo("MY-RELAXED-PROPERTY"));
		assertThat(iterator.next(), equalTo("MY_RELAXED_PROPERTY"));
		assertThat(iterator.next(), equalTo("MYRELAXEDPROPERTY"));
		assertThat(iterator.hasNext(), equalTo(false));
	}

	@Test
	public void fromUnderscores() throws Exception {
		Iterator<String> iterator = new RelaxedNames("nes_ted").iterator();
		assertThat(iterator.next(), equalTo("nes_ted"));
		assertThat(iterator.next(), equalTo("nes.ted"));
		assertThat(iterator.next(), equalTo("nesTed"));
		assertThat(iterator.next(), equalTo("nested"));
		assertThat(iterator.next(), equalTo("NES_TED"));
		assertThat(iterator.next(), equalTo("NES.TED"));
		assertThat(iterator.next(), equalTo("NESTED"));
		assertThat(iterator.hasNext(), equalTo(false));
	}

	@Test
	public void fromPlain() throws Exception {
		Iterator<String> iterator = new RelaxedNames("plain").iterator();
		assertThat(iterator.next(), equalTo("plain"));
		assertThat(iterator.next(), equalTo("PLAIN"));
		assertThat(iterator.hasNext(), equalTo(false));
	}

	@Test
	public void fromCamelCase() throws Exception {
		Iterator<String> iterator = new RelaxedNames("caMel").iterator();
		assertThat(iterator.next(), equalTo("caMel"));
		assertThat(iterator.next(), equalTo("ca_mel"));
		assertThat(iterator.next(), equalTo("camel"));
		assertThat(iterator.next(), equalTo("CAMEL"));
		assertThat(iterator.next(), equalTo("CA_MEL"));
		assertThat(iterator.hasNext(), equalTo(false));
	}

	@Test
	public void fromPeriods() throws Exception {
		Iterator<String> iterator = new RelaxedNames("spring.value").iterator();
		assertThat(iterator.next(), equalTo("spring.value"));
		assertThat(iterator.next(), equalTo("spring_value"));
		assertThat(iterator.next(), equalTo("springValue"));
		assertThat(iterator.next(), equalTo("springvalue"));
		assertThat(iterator.next(), equalTo("SPRING.VALUE"));
		assertThat(iterator.next(), equalTo("SPRING_VALUE"));
		assertThat(iterator.next(), equalTo("SPRINGVALUE"));
		assertThat(iterator.hasNext(), equalTo(false));
	}

	@Test
	public void fromPrefixEndingInPeriod() throws Exception {
		Iterator<String> iterator = new RelaxedNames("spring.").iterator();
		assertThat(iterator.next(), equalTo("spring."));
		assertThat(iterator.next(), equalTo("spring_"));
		assertThat(iterator.next(), equalTo("SPRING."));
		assertThat(iterator.next(), equalTo("SPRING_"));
		assertThat(iterator.hasNext(), equalTo(false));
	}

	@Test
	public void fromEmpty() throws Exception {
		Iterator<String> iterator = new RelaxedNames("").iterator();
		assertThat(iterator.next(), equalTo(""));
		assertThat(iterator.hasNext(), equalTo(false));
	}

}
