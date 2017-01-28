/*
 *
 *  * Copyright 2012-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.boot.bind;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameter;


/**
 * Tests for {@link CanonicalSystemEnvironmentPropertySource}.
 * @author Madhura Bhave
 */
@RunWith(Parameterized.class)
public class CanonicalSystemEnvironmentPropertySourceTests {

	private CanonicalSystemEnvironmentPropertySource propertySource;

	@Parameter(0)
	public NameValue input;

	@Parameter(1)
	public NameValue[] expected;

	@Before
	public void setUp() throws Exception {
		this.propertySource = new CanonicalSystemEnvironmentPropertySource("test", input.asMap());
	}

	@Test
	public void containsPropertyShouldContainInput() throws Exception {
		assertThat(propertySource.containsProperty(input.getName())).isTrue();
	}

	@Test
	public void containsPropertyShouldContainExpected() throws Exception {
		for(NameValue item: expected) {
			assertThat(propertySource.containsProperty(item.getName())).isTrue();
		}
	}

	@Test
	public void getPropertyShouldContainInput() throws Exception {
		assertThat(propertySource.getProperty(input.getName())).isEqualTo(input.getValue());
	}

	@Test
	public void getPropertyShouldContainExpected() throws Exception {
		for(NameValue item: expected) {
			assertThat(propertySource.getProperty(item.getName())).isEqualTo(item.getValue());
		}
	}

	@Test
	public void getCanonicalPropertyNamesShouldContainOnlyExpectedNames() throws Exception {
		String[] names = Arrays.stream(expected).map(NameValue::getName).toArray(size -> new String[size]);
		assertThat(propertySource.getCanonicalPropertyNames()).containsExactlyInAnyOrder(names);
	}

	@Parameters
	public static Object[] parameters() {
		List<Object> parameters = new ArrayList<>();
		parameters.add(parameter("FOO=bar", "foo=bar"));
		parameters.add(parameter("FOO_BAR=baz", "foo.bar=baz"));
		parameters.add(parameter("FOO_BAR_1_=baz", "foo.bar[1]=baz"));
		parameters.add(parameter("FOO_BAR_1=baz", "foo.bar[1]=baz"));
		parameters.add(parameter("FOO_BAR_1_BING=baz", "foo.bar[1].bing=baz"));
		parameters.add(parameter("FOO_BAR_1_2_=baz", "foo.bar[1][2]=baz"));
		parameters.add(parameter("FOO_BAR1_2_=baz", "foo.bar1[2]=baz"));
		parameters.add(parameter("FOO_BAR_1_2=baz", "foo.bar[1][2]=baz"));
		parameters.add(parameter("_BAR=baz", "bar=baz"));
		parameters.add(parameter("__BAR=baz", "bar=baz"));
		parameters.add(parameter("__BAR___=baz", "bar=baz"));
		parameters.add(parameter("_1_2=baz", "[1][2]=baz"));
		parameters.add(parameter("1_2=baz", "[1][2]=baz"));

//		parameters.add(parameter("FOO__BAR__BING=baz", "foo[bar].bing=baz"));
//		parameters.add(parameter("FOO__BAR_RAB__BING=baz", "foo[bar.rab].bing=baz"));
//		parameters.add(parameter("FOO__BAR_RAB__BING__=baz", "foo[bar.rab].bing[0]=baz"));
//		parameters.add(parameter("FOO__BAR_RAB__BING__=baz,boo", "foo[bar.rab].bing[0]=baz", "foo[bar.rab].bing[1]=boo"));
//		parameters.add(parameter("FOO__BAR____RAB__BING=baz", "foo[bar][rab].bing=baz"));
//		parameters.add(parameter("FOO_BAR__=baz", "foo.bar[0]=baz"));
//		parameters.add(parameter("FOO_BAR__=baz,bat", "foo.bar[0]=baz", "foo.bar[1]=bat"));
//		parameters.add(parameter("FOO_BAR1_2___=baz,boo", "foo.bar1[2][0]=baz", "foo.bar1[2][1]=boo"));
		return parameters.toArray();
	}

	private static Object[] parameter(String input, String... expected) {
		Object[] parameter = new Object[2];
		parameter[0] = NameValue.parse(input);
		parameter[1] = NameValue.parse(expected);
		return parameter;
	}

	private static class NameValue {

		private final String name;

		private final String value;

		NameValue(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return this.name;
		}

		public String getValue() {
			return this.value;
		}

		public static NameValue[] parse(String[] strings) {
			NameValue[] nameValues = new NameValue[strings.length];
			for (int i = 0; i < nameValues.length; i++) {
				nameValues[i] = parse(strings[i]);
			}
			return nameValues;
		}

		private static NameValue parse(String string) {
			String[] split = string.split("=");
			return new NameValue(split[0], split[1]);
		}

		public Map<String,Object> asMap() {
			return Collections.singletonMap(name,value);
		}
	}



}