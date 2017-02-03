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

package org.springframework.boot.env.fixme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.boot.env.fixme.PropertiesCanonicalPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Madhura Bhave
 */
@RunWith(Parameterized.class)
public class CanonicalPropertyFilePropertySourceTests {

	@Parameter
	public NameValue input;

	@Parameter(1)
	public NameValue[] expected;

	private PropertiesCanonicalPropertySource propertySource;

	@Before
	public void setup() {
		Properties properties = new Properties();
		properties.put(this.input.getName(), this.input.getValue());
		this.propertySource = new PropertiesCanonicalPropertySource("test", properties);
	}

	@Parameters
	public static Object[] parameters() {
		List<Object> parameters = new ArrayList();
		parameters.add(parameter("foo.bar=baz", "foo.bar=baz"));
		parameters.add(parameter("foo.BAR=baz", "foo.bar=baz"));
		parameters.add(parameter("foo.bar_blink=baz", "foo.barblink=baz"));
		parameters.add(parameter("foo.bar-blink=baz", "foo.barblink=baz"));
		parameters.add(parameter("foo.bar-blink.baz=bat", "foo.barblink.baz=bat"));
		parameters.add(parameter("foo.bar[0]=baz", "foo.bar[0]=baz"));
		parameters.add(parameter("foo.bar[1]=baz", "foo.bar[1]=baz"));
		parameters.add(parameter("foo.bar[0][1]=baz", "foo.bar[0][1]=baz"));
		parameters.add(parameter("foo.bar[]=baz,bling", "foo.bar[0]=baz", "foo.bar[1]=bling"));
		return parameters.toArray();
	}

	private static Object[] parameter(String input, String... expected) {
		Object[] parameter = new Object[2];
		parameter[0] = NameValue.parse(input);
		parameter[1] = NameValue.parse(expected);
		return parameter;
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

	}


}
