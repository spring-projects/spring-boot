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

package org.springframework.boot.context.properties.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Element;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link ConfigurationPropertyName}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class ConfigurationPropertyNameTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void ofNameShouldNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		ConfigurationPropertyName.of((String) null);
	}

	@Test
	public void ofNameShouldNotStartWithNumber() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("1foo");
	}

	@Test
	public void ofNameShouldNotStartWithDash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("-foo");
	}

	@Test
	public void ofNameShouldNotStartWithDot() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not start with '.'");
		ConfigurationPropertyName.of(".foo");
	}

	@Test
	public void ofNameShouldNotEndWithDot() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not end with '.'");
		ConfigurationPropertyName.of("foo.");
	}

	@Test
	public void ofNameShouldNotContainUppercase() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("fOo");
	}

	@Test
	public void ofNameShouldNotContainInvalidChars() throws Exception {
		String invalid = "_@$%*+=':;";
		for (char c : invalid.toCharArray()) {
			try {
				ConfigurationPropertyName.of("foo" + c);
				fail("Did not throw for invalid char " + c);
			}
			catch (IllegalArgumentException ex) {
				assertThat(ex.getMessage()).contains("is not valid");
			}
		}
	}

	@Test
	public void ofNameWhenSimple() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("name");
		assertThat(name.toString()).isEqualTo("name");
		assertThat((Object) name.getParent()).isNull();
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("name");
	}

	@Test
	public void ofNameWhenRunOnAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[bar]");
		assertThat(name.toString()).isEqualTo("foo[bar]");
		assertThat(name.getParent().toString()).isEqualTo("foo");
		assertThat(name.getElement().toString()).isEqualTo("[bar]");
	}

	@Test
	public void ofNameWhenDotOnAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.toString()).isEqualTo("foo.bar");
		assertThat(name.getParent().toString()).isEqualTo("foo");
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("bar");
	}

	@Test
	public void ofNameWhenDotAndAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.[bar]");
		assertThat(name.toString()).isEqualTo("foo[bar]");
		assertThat(name.getParent().toString()).isEqualTo("foo");
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("bar");
	}

	@Test
	public void ofNameWhenDoubleRunOnAndAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[bar]baz");
		assertThat(name.toString()).isEqualTo("foo[bar].baz");
		assertThat(name.getParent().toString()).isEqualTo("foo[bar]");
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("baz");
	}

	@Test
	public void ofNameWhenDoubleDotAndAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.[bar].baz");
		assertThat(name.toString()).isEqualTo("foo[bar].baz");
		assertThat(name.getParent().toString()).isEqualTo("foo[bar]");
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("baz");
	}

	@Test
	public void ofNameWhenMissingCloseBracket() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("[bar");
	}

	@Test
	public void ofNameWhenMissingOpenBracket() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("bar]");
	}

	@Test
	public void ofNameWithWhitespaceInName() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("foo. bar");
	}

	@Test
	public void ofNameWithWhitespaceInAssociativeElement() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[b a r]");
		assertThat(name.toString()).isEqualTo("foo[b a r]");
		assertThat(name.getParent().toString()).isEqualTo("foo");
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("b a r");
	}

	@Test
	public void ofNameWithUppercaseInAssociativeElement() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[BAR]");
		assertThat(name.toString()).isEqualTo("foo[BAR]");
		assertThat(name.getParent().toString()).isEqualTo("foo");
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("BAR");
	}

	@Test
	public void equalsAndHashCode() throws Exception {
		ConfigurationPropertyName name1 = ConfigurationPropertyName.of("foo[bar]");
		ConfigurationPropertyName name2 = ConfigurationPropertyName.of("foo[bar]");
		ConfigurationPropertyName name3 = ConfigurationPropertyName.of("foo.bar");
		ConfigurationPropertyName name4 = ConfigurationPropertyName.of("f-o-o.b-a-r");
		ConfigurationPropertyName name5 = ConfigurationPropertyName.of("foo[BAR]");
		ConfigurationPropertyName name6 = ConfigurationPropertyName.of("oof[bar]");
		ConfigurationPropertyName name7 = ConfigurationPropertyName.of("foo.bar");
		ConfigurationPropertyName name8 = new ConfigurationPropertyName(
				new ConfigurationPropertyName(null, new Element("FOO")),
				new Element("BAR"));
		assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
		assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
		assertThat(name1.hashCode()).isEqualTo(name3.hashCode());
		assertThat(name1.hashCode()).isEqualTo(name4.hashCode());
		assertThat(name7.hashCode()).isEqualTo(name8.hashCode());
		assertThat((Object) name1).isEqualTo(name1);
		assertThat((Object) name1).isEqualTo(name2);
		assertThat((Object) name1).isEqualTo(name3);
		assertThat((Object) name1).isEqualTo(name4);
		assertThat((Object) name1).isNotEqualTo(name5);
		assertThat((Object) name1).isNotEqualTo(name6);
		assertThat((Object) name7).isEqualTo(name8);
	}

	@Test
	public void elementNameShouldNotIncludeAngleBrackets() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("[foo]");
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("foo");
	}

	@Test
	public void elementNameShouldNotIncludeDashes() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("f-o-o");
		assertThat(name.getElement().getValue(Form.UNIFORM)).isEqualTo("foo");
	}

	@Test
	public void streamShouldReturnElements() throws Exception {
		assertThat(streamElements("foo.bar")).containsExactly("foo", "bar");
		assertThat(streamElements("foo[0]")).containsExactly("foo", "[0]");
		assertThat(streamElements("foo.[0]")).containsExactly("foo", "[0]");
		assertThat(streamElements("foo[baz]")).containsExactly("foo", "[baz]");
		assertThat(streamElements("foo.baz")).containsExactly("foo", "baz");
		assertThat(streamElements("foo[baz].bar")).containsExactly("foo", "[baz]", "bar");
		assertThat(streamElements("foo.baz.bar")).containsExactly("foo", "baz", "bar");
		assertThat(streamElements("foo.baz-bar")).containsExactly("foo", "baz-bar");
	}

	private Iterator<String> streamElements(String name) {
		return ConfigurationPropertyName.of(name).stream().map((e) -> e.toString())
				.iterator();
	}

	@Test
	public void elementIsIndexedWhenIndexedShouldReturnTrue() throws Exception {
		assertThat(ConfigurationPropertyName.of("foo[0]").getElement().isIndexed())
				.isTrue();
	}

	@Test
	public void elementIsIndexedWhenNotIndexedShouldReturnFalse() throws Exception {
		assertThat(ConfigurationPropertyName.of("foo.bar").getElement().isIndexed())
				.isFalse();
	}

	@Test
	public void isAncestorOfWhenSameShouldReturnFalse() throws Exception {
		ConfigurationPropertyName parent = ConfigurationPropertyName.of("foo");
		assertThat(parent.isAncestorOf(parent)).isFalse();
	}

	@Test
	public void isAncestorOfWhenParentShouldReturnFalse() throws Exception {
		ConfigurationPropertyName parent = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName child = ConfigurationPropertyName.of("foo.bar");
		assertThat(parent.isAncestorOf(child)).isTrue();
		assertThat(child.isAncestorOf(parent)).isFalse();
	}

	@Test
	public void isAncestorOfWhenGrandparentShouldReturnFalse() throws Exception {
		ConfigurationPropertyName parent = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName
				.of("foo.bar.baz");
		assertThat(parent.isAncestorOf(grandchild)).isTrue();
		assertThat(grandchild.isAncestorOf(parent)).isFalse();
	}

	@Test
	public void appendWhenNotIndexedShouldAppendWithDot() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat(name.append("bar").toString()).isEqualTo("foo.bar");
	}

	@Test
	public void appendWhenIndexedShouldAppendWithBrackets() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo")
				.append("[bar]");
		assertThat(name.getElement().isIndexed()).isTrue();
		assertThat(name.toString()).isEqualTo("foo[bar]");
	}

	@Test
	public void appendWhenElementNameIsNotValidShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Element value '1bar' is not valid");
		ConfigurationPropertyName.of("foo").append("1bar");
	}

	@Test
	public void appendWhenElementNameIsNullShouldReturnName() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat((Object) name.append((String) null)).isSameAs(name);
	}

	@Test
	public void compareShouldSortNames() throws Exception {
		List<ConfigurationPropertyName> names = new ArrayList<>();
		names.add(ConfigurationPropertyName.of("foo[10]"));
		names.add(ConfigurationPropertyName.of("foo.bard"));
		names.add(ConfigurationPropertyName.of("foo[2]"));
		names.add(ConfigurationPropertyName.of("foo.bar"));
		names.add(ConfigurationPropertyName.of("foo.baz"));
		names.add(ConfigurationPropertyName.of("foo"));
		Collections.sort(names);
		assertThat(names.stream().map(ConfigurationPropertyName::toString)
				.collect(Collectors.toList())).containsExactly("foo", "foo[2]", "foo[10]",
						"foo.bar", "foo.bard", "foo.baz");
	}

	@Test
	public void ofNameCanBeEmpty() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("");
		assertThat(name.toString()).isEqualTo("");
		assertThat(name.append("foo").toString()).isEqualTo("foo");
	}

}
