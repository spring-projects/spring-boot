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

package org.springframework.boot.env;

import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OriginTrackedPropertiesLoader}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class OriginTrackedPropertiesLoaderTests {

	private ClassPathResource resource;

	private Map<String, OriginTrackedValue> properties;

	@Before
	public void setUp() throws Exception {
		String path = "test-properties.properties";
		this.resource = new ClassPathResource(path, getClass());
		this.properties = new OriginTrackedPropertiesLoader(this.resource).load();
	}

	@Test
	public void compareToJavaProperties() throws Exception {
		Properties java = PropertiesLoaderUtils.loadProperties(this.resource);
		Properties ours = new Properties();
		new OriginTrackedPropertiesLoader(this.resource).load(false)
				.forEach((k, v) -> ours.put(k, v.getValue()));
		assertThat(ours).isEqualTo(java);
	}

	@Test
	public void getSimpleProperty() {
		OriginTrackedValue value = this.properties.get("test");
		assertThat(getValue(value)).isEqualTo("properties");
		assertThat(getLocation(value)).isEqualTo("11:6");
	}

	@Test
	public void getSimplePropertyWithColonSeparator() {
		OriginTrackedValue value = this.properties.get("test-colon-separator");
		assertThat(getValue(value)).isEqualTo("my-property");
		assertThat(getLocation(value)).isEqualTo("15:23");
	}

	@Test
	public void getPropertyWithSeparatorSurroundedBySpaces() {
		OriginTrackedValue value = this.properties.get("blah");
		assertThat(getValue(value)).isEqualTo("hello world");
		assertThat(getLocation(value)).isEqualTo("2:12");
	}

	@Test
	public void getUnicodeProperty() {
		OriginTrackedValue value = this.properties.get("test-unicode");
		assertThat(getValue(value)).isEqualTo("properties&test");
		assertThat(getLocation(value)).isEqualTo("12:14");
	}

	@Test
	public void getEscapedProperty() {
		OriginTrackedValue value = this.properties.get("test=property");
		assertThat(getValue(value)).isEqualTo("helloworld");
		assertThat(getLocation(value)).isEqualTo("14:15");
	}

	@Test
	public void getPropertyWithTab() {
		OriginTrackedValue value = this.properties.get("test-tab-property");
		assertThat(getValue(value)).isEqualTo("foo\tbar");
		assertThat(getLocation(value)).isEqualTo("16:19");
	}

	@Test
	public void getPropertyWithBang() {
		OriginTrackedValue value = this.properties.get("test-bang-property");
		assertThat(getValue(value)).isEqualTo("foo!");
		assertThat(getLocation(value)).isEqualTo("34:20");
	}

	@Test
	public void getPropertyWithValueComment() {
		OriginTrackedValue value = this.properties.get("test-property-value-comment");
		assertThat(getValue(value)).isEqualTo("foo !bar #foo");
		assertThat(getLocation(value)).isEqualTo("36:29");
	}

	@Test
	public void getPropertyWithMultilineImmediateBang() {
		OriginTrackedValue value = this.properties.get("test-multiline-immediate-bang");
		assertThat(getValue(value)).isEqualTo("!foo");
		assertThat(getLocation(value)).isEqualTo("39:1");
	}

	@Test
	public void getPropertyWithCarriageReturn() {
		OriginTrackedValue value = this.properties.get("test-return-property");
		assertThat(getValue(value)).isEqualTo("foo\rbar");
		assertThat(getLocation(value)).isEqualTo("17:22");
	}

	@Test
	public void getPropertyWithNewLine() {
		OriginTrackedValue value = this.properties.get("test-newline-property");
		assertThat(getValue(value)).isEqualTo("foo\nbar");
		assertThat(getLocation(value)).isEqualTo("18:23");
	}

	@Test
	public void getPropertyWithFormFeed() {
		OriginTrackedValue value = this.properties.get("test-form-feed-property");
		assertThat(getValue(value)).isEqualTo("foo\fbar");
		assertThat(getLocation(value)).isEqualTo("19:25");
	}

	@Test
	public void getPropertyWithWhiteSpace() {
		OriginTrackedValue value = this.properties.get("test-whitespace-property");
		assertThat(getValue(value)).isEqualTo("foo   bar");
		assertThat(getLocation(value)).isEqualTo("20:32");
	}

	@Test
	public void getCommentedOutPropertyShouldBeNull() {
		assertThat(this.properties.get("commented-property")).isNull();
		assertThat(this.properties.get("#commented-property")).isNull();
		assertThat(this.properties.get("commented-two")).isNull();
		assertThat(this.properties.get("!commented-two")).isNull();
	}

	@Test
	public void getMultiline() {
		OriginTrackedValue value = this.properties.get("test-multiline");
		assertThat(getValue(value)).isEqualTo("ab\\c");
		assertThat(getLocation(value)).isEqualTo("21:17");
	}

	@Test
	public void getImmediateMultiline() {
		OriginTrackedValue value = this.properties.get("test-multiline-immediate");
		assertThat(getValue(value)).isEqualTo("foo");
		assertThat(getLocation(value)).isEqualTo("32:1");
	}

	@Test
	public void getPropertyWithWhitespaceAfterKey() {
		OriginTrackedValue value = this.properties.get("bar");
		assertThat(getValue(value)).isEqualTo("foo=baz");
		assertThat(getLocation(value)).isEqualTo("3:7");
	}

	@Test
	public void getPropertyWithSpaceSeparator() {
		OriginTrackedValue value = this.properties.get("hello");
		assertThat(getValue(value)).isEqualTo("world");
		assertThat(getLocation(value)).isEqualTo("4:9");
	}

	@Test
	public void getPropertyWithBackslashEscaped() {
		OriginTrackedValue value = this.properties.get("proper\\ty");
		assertThat(getValue(value)).isEqualTo("test");
		assertThat(getLocation(value)).isEqualTo("5:11");
	}

	@Test
	public void getPropertyWithEmptyValue() {
		OriginTrackedValue value = this.properties.get("foo");
		assertThat(getValue(value)).isEqualTo("");
		assertThat(getLocation(value)).isEqualTo("7:0");
	}

	@Test
	public void getPropertyWithBackslashEscapedInValue() {
		OriginTrackedValue value = this.properties.get("bat");
		assertThat(getValue(value)).isEqualTo("a\\");
		assertThat(getLocation(value)).isEqualTo("7:7");
	}

	@Test
	public void getPropertyWithSeparatorInValue() {
		OriginTrackedValue value = this.properties.get("bling");
		assertThat(getValue(value)).isEqualTo("a=b");
		assertThat(getLocation(value)).isEqualTo("8:9");
	}

	@Test
	public void getListProperty() {
		OriginTrackedValue apple = this.properties.get("foods[0]");
		assertThat(getValue(apple)).isEqualTo("Apple");
		assertThat(getLocation(apple)).isEqualTo("24:9");
		OriginTrackedValue orange = this.properties.get("foods[1]");
		assertThat(getValue(orange)).isEqualTo("Orange");
		assertThat(getLocation(orange)).isEqualTo("25:1");
		OriginTrackedValue strawberry = this.properties.get("foods[2]");
		assertThat(getValue(strawberry)).isEqualTo("Strawberry");
		assertThat(getLocation(strawberry)).isEqualTo("26:1");
		OriginTrackedValue mango = this.properties.get("foods[3]");
		assertThat(getValue(mango)).isEqualTo("Mango");
		assertThat(getLocation(mango)).isEqualTo("27:1");
	}

	@Test
	public void getPropertyWithISO88591Character() {
		OriginTrackedValue value = this.properties.get("test-iso8859-1-chars");
		assertThat(getValue(value)).isEqualTo("æ×ÈÅÞßáñÀÿ");
	}

	private Object getValue(OriginTrackedValue value) {
		return (value == null ? null : value.getValue());
	}

	private String getLocation(OriginTrackedValue value) {
		if (value == null) {
			return null;
		}
		return ((TextResourceOrigin) value.getOrigin()).getLocation().toString();
	}

}
