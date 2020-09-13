/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationPropertyName}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Eddú Meléndez
 */
class ConfigurationPropertyNameTests {

	@Test
	void ofNameShouldNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> ConfigurationPropertyName.of(null))
				.withMessageContaining("Name must not be null");
	}

	@Test
	void ofNameShouldNotStartWithDash() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of("-foo")).withMessageContaining("is not valid");
	}

	@Test
	void ofNameShouldNotStartWithDot() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of(".foo")).withMessageContaining("is not valid");
	}

	@Test
	void ofNameShouldNotEndWithDot() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of("foo.")).withMessageContaining("is not valid");
	}

	@Test
	void ofNameShouldNotContainUppercase() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of("fOo")).withMessageContaining("is not valid");
	}

	@Test
	void ofNameShouldNotContainInvalidChars() {
		String invalid = "_@$%*+=':;";
		for (char c : invalid.toCharArray()) {
			assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
					.isThrownBy(() -> ConfigurationPropertyName.of("foo" + c))
					.satisfies((ex) -> assertThat(ex.getMessage()).contains("is not valid"));
		}
	}

	@Test
	void ofNameWhenSimple() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("name");
		assertThat(name.toString()).isEqualTo("name");
		assertThat(name.getNumberOfElements()).isEqualTo(1);
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("name");
		assertThat(name.isIndexed(0)).isFalse();
	}

	@Test
	void ofNameWhenStartsWithNumber() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("1foo");
		assertThat(name.toString()).isEqualTo("1foo");
		assertThat(name.getNumberOfElements()).isEqualTo(1);
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("1foo");
		assertThat(name.isIndexed(0)).isFalse();
	}

	@Test
	void ofNameWhenRunOnAssociative() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[bar]");
		assertThat(name.toString()).isEqualTo("foo[bar]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
	}

	@Test
	void ofNameWhenDotOnAssociative() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.toString()).isEqualTo("foo.bar");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isFalse();
	}

	@Test
	void ofNameWhenDotAndAssociative() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.[bar]");
		assertThat(name.toString()).isEqualTo("foo[bar]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
	}

	@Test
	void ofNameWhenDoubleRunOnAndAssociative() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[bar]baz");
		assertThat(name.toString()).isEqualTo("foo[bar].baz");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.getElement(2, Form.ORIGINAL)).isEqualTo("baz");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
		assertThat(name.isIndexed(2)).isFalse();
	}

	@Test
	void ofNameWhenDoubleDotAndAssociative() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.[bar].baz");
		assertThat(name.toString()).isEqualTo("foo[bar].baz");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.getElement(2, Form.ORIGINAL)).isEqualTo("baz");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
		assertThat(name.isIndexed(2)).isFalse();
	}

	@Test
	void ofNameWhenMissingCloseBracket() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of("[bar")).withMessageContaining("is not valid");
	}

	@Test
	void ofNameWhenMissingOpenBracket() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of("bar]")).withMessageContaining("is not valid");
	}

	@Test
	void ofNameWhenMultipleMismatchedBrackets() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of("[a[[[b]ar]")).withMessageContaining("is not valid");
	}

	@Test
	void ofNameWhenNestedBrackets() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[a[c][[b]ar]]");
		assertThat(name.toString()).isEqualTo("foo[a[c][[b]ar]]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("a[c][[b]ar]");
	}

	@Test
	void ofNameWithWhitespaceInName() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of("foo. bar")).withMessageContaining("is not valid");
	}

	@Test
	void ofNameWithWhitespaceInAssociativeElement() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[b a r]");
		assertThat(name.toString()).isEqualTo("foo[b a r]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("b a r");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
	}

	@Test
	void ofNameWithUppercaseInAssociativeElement() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[BAR]");
		assertThat(name.toString()).isEqualTo("foo[BAR]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("BAR");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
	}

	@Test
	void ofWhenNameIsEmptyShouldReturnEmptyName() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("");
		assertThat(name.toString()).isEqualTo("");
		assertThat(name.append("foo").toString()).isEqualTo("foo");
	}

	@Test
	void adaptWhenNameIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ConfigurationPropertyName.adapt(null, '.'))
				.withMessageContaining("Name must not be null");
	}

	@Test
	void adaptWhenElementValueProcessorIsNullShouldAdapt() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("foo", '.', null);
		assertThat(name.toString()).isEqualTo("foo");
	}

	@Test
	void adaptShouldCreateName() {
		ConfigurationPropertyName expected = ConfigurationPropertyName.of("foo.bar.baz");
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("foo.bar.baz", '.');
		assertThat(name).isEqualTo(expected);
	}

	@Test
	void adaptShouldStripInvalidChars() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("f@@.b%r", '.');
		assertThat(name.getElement(0, Form.UNIFORM)).isEqualTo("f");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("f");
		assertThat(name.getElement(1, Form.UNIFORM)).isEqualTo("br");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("br");
		assertThat(name.toString()).isEqualTo("f.br");
	}

	@Test
	void adaptShouldSupportUnderscore() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("f-_o.b_r", '.');
		assertThat(name.getElement(0, Form.UNIFORM)).isEqualTo("fo");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("f-_o");
		assertThat(name.getElement(1, Form.UNIFORM)).isEqualTo("br");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("b_r");
		assertThat(name.toString()).isEqualTo("f-o.br");
	}

	@Test
	void adaptShouldSupportMixedCase() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("fOo.bAr", '.');
		assertThat(name.getElement(0, Form.UNIFORM)).isEqualTo("foo");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("fOo");
		assertThat(name.getElement(1, Form.UNIFORM)).isEqualTo("bar");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bAr");
		assertThat(name.toString()).isEqualTo("foo.bar");
	}

	@Test
	void adaptShouldUseElementValueProcessor() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("FOO_THE-BAR", '_',
				(c) -> c.toString().replace("-", ""));
		assertThat(name.toString()).isEqualTo("foo.thebar");
	}

	@Test
	void adaptShouldSupportIndexedElements() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("foo", '.');
		assertThat(name.toString()).isEqualTo("foo");
		assertThat(name.getNumberOfElements()).isEqualTo(1);
		name = ConfigurationPropertyName.adapt("[foo]", '.');
		assertThat(name.toString()).isEqualTo("[foo]");
		assertThat(name.getNumberOfElements()).isEqualTo(1);
		name = ConfigurationPropertyName.adapt("foo.bar", '.');
		assertThat(name.toString()).isEqualTo("foo.bar");
		assertThat(name.getNumberOfElements()).isEqualTo(2);
		name = ConfigurationPropertyName.adapt("foo[foo.bar]", '.');
		assertThat(name.toString()).isEqualTo("foo[foo.bar]");
		assertThat(name.getNumberOfElements()).isEqualTo(2);
		name = ConfigurationPropertyName.adapt("foo.[bar].baz", '.');
		assertThat(name.toString()).isEqualTo("foo[bar].baz");
		assertThat(name.getNumberOfElements()).isEqualTo(3);
	}

	@Test
	void adaptUnderscoreShouldReturnEmpty() {
		assertThat(ConfigurationPropertyName.adapt("_", '_').isEmpty()).isTrue();
		assertThat(ConfigurationPropertyName.adapt("_", '.').isEmpty()).isTrue();
	}

	@Test
	void isEmptyWhenEmptyShouldReturnTrue() {
		assertThat(ConfigurationPropertyName.of("").isEmpty()).isTrue();
	}

	@Test
	void isEmptyWhenNotEmptyShouldReturnFalse() {
		assertThat(ConfigurationPropertyName.of("x").isEmpty()).isFalse();
	}

	@Test
	void isLastElementIndexedWhenIndexedShouldReturnTrue() {
		assertThat(ConfigurationPropertyName.of("foo[0]").isLastElementIndexed()).isTrue();
	}

	@Test
	void isLastElementIndexedWhenNotIndexedShouldReturnFalse() {
		assertThat(ConfigurationPropertyName.of("foo.bar").isLastElementIndexed()).isFalse();
		assertThat(ConfigurationPropertyName.of("foo[0].bar").isLastElementIndexed()).isFalse();
	}

	@Test
	void getLastElementShouldGetLastElement() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("foo.bAr", '.');
		assertThat(name.getLastElement(Form.ORIGINAL)).isEqualTo("bAr");
		assertThat(name.getLastElement(Form.UNIFORM)).isEqualTo("bar");
	}

	@Test
	void getLastElementWhenEmptyShouldReturnEmptyString() {
		ConfigurationPropertyName name = ConfigurationPropertyName.EMPTY;
		assertThat(name.getLastElement(Form.ORIGINAL)).isEqualTo("");
		assertThat(name.getLastElement(Form.UNIFORM)).isEqualTo("");
	}

	@Test
	void getElementShouldNotIncludeAngleBrackets() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("[foo]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(0, Form.UNIFORM)).isEqualTo("foo");
	}

	@Test
	void getElementInUniformFormShouldNotIncludeDashes() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("f-o-o");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("f-o-o");
		assertThat(name.getElement(0, Form.UNIFORM)).isEqualTo("foo");
	}

	@Test
	void getElementInOriginalFormShouldReturnElement() {
		assertThat(getElements("foo.bar", Form.ORIGINAL)).containsExactly("foo", "bar");
		assertThat(getElements("foo[0]", Form.ORIGINAL)).containsExactly("foo", "0");
		assertThat(getElements("foo.[0]", Form.ORIGINAL)).containsExactly("foo", "0");
		assertThat(getElements("foo[baz]", Form.ORIGINAL)).containsExactly("foo", "baz");
		assertThat(getElements("foo.baz", Form.ORIGINAL)).containsExactly("foo", "baz");
		assertThat(getElements("foo[baz].bar", Form.ORIGINAL)).containsExactly("foo", "baz", "bar");
		assertThat(getElements("foo.baz.bar", Form.ORIGINAL)).containsExactly("foo", "baz", "bar");
		assertThat(getElements("foo.baz-bar", Form.ORIGINAL)).containsExactly("foo", "baz-bar");
	}

	@Test
	void getElementInUniformFormShouldReturnElement() {
		assertThat(getElements("foo.bar", Form.UNIFORM)).containsExactly("foo", "bar");
		assertThat(getElements("foo[0]", Form.UNIFORM)).containsExactly("foo", "0");
		assertThat(getElements("foo.[0]", Form.UNIFORM)).containsExactly("foo", "0");
		assertThat(getElements("foo[baz]", Form.UNIFORM)).containsExactly("foo", "baz");
		assertThat(getElements("foo.baz", Form.UNIFORM)).containsExactly("foo", "baz");
		assertThat(getElements("foo[baz].bar", Form.UNIFORM)).containsExactly("foo", "baz", "bar");
		assertThat(getElements("foo.baz.bar", Form.UNIFORM)).containsExactly("foo", "baz", "bar");
		assertThat(getElements("foo.baz-bar", Form.UNIFORM)).containsExactly("foo", "bazbar");
	}

	private List<CharSequence> getElements(String name, Form form) {
		ConfigurationPropertyName propertyName = ConfigurationPropertyName.of(name);
		List<CharSequence> result = new ArrayList<>(propertyName.getNumberOfElements());
		for (int i = 0; i < propertyName.getNumberOfElements(); i++) {
			result.add(propertyName.getElement(i, form));
		}
		return result;
	}

	@Test
	void getNumberOfElementsShouldReturnNumberOfElement() {
		assertThat(ConfigurationPropertyName.of("").getNumberOfElements()).isEqualTo(0);
		assertThat(ConfigurationPropertyName.of("x").getNumberOfElements()).isEqualTo(1);
		assertThat(ConfigurationPropertyName.of("x.y").getNumberOfElements()).isEqualTo(2);
		assertThat(ConfigurationPropertyName.of("x[0].y").getNumberOfElements()).isEqualTo(3);
	}

	@Test
	void appendWhenNotIndexedShouldAppendWithDot() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat(name.append("bar").toString()).isEqualTo("foo.bar");
	}

	@Test
	void appendWhenIndexedShouldAppendWithBrackets() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo").append("[bar]");
		assertThat(name.isLastElementIndexed()).isTrue();
		assertThat(name.toString()).isEqualTo("foo[bar]");
	}

	@Test
	void appendWhenElementNameIsNotValidShouldThrowException() {
		assertThatExceptionOfType(InvalidConfigurationPropertyNameException.class)
				.isThrownBy(() -> ConfigurationPropertyName.of("foo").append("-bar"))
				.withMessageContaining("Configuration property name '-bar' is not valid");
	}

	@Test
	void appendWhenElementNameMultiDotShouldAppend() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo").append("bar.baz");
		assertThat(name.toString()).isEqualTo("foo.bar.baz");
		assertThat(name.getNumberOfElements()).isEqualTo(3);
	}

	@Test
	void appendWhenElementNameIsNullShouldReturnName() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat((Object) name.append(null)).isSameAs(name);
	}

	@Test
	void chopWhenLessThenSizeShouldReturnChopped() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.bar.baz");
		assertThat(name.chop(1).toString()).isEqualTo("foo");
		assertThat(name.chop(2).toString()).isEqualTo("foo.bar");
	}

	@Test
	void chopWhenGreaterThanSizeShouldReturnExisting() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.bar.baz");
		assertThat(name.chop(4)).isEqualTo(name);
	}

	@Test
	void chopWhenEqualToSizeShouldReturnExisting() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.bar.baz");
		assertThat(name.chop(3)).isEqualTo(name);
	}

	@Test
	void isParentOfWhenSameShouldReturnFalse() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat(name.isParentOf(name)).isFalse();
	}

	@Test
	void isParentOfWhenParentShouldReturnTrue() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName child = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.isParentOf(child)).isTrue();
		assertThat(child.isParentOf(name)).isFalse();
	}

	@Test
	void isParentOfWhenGrandparentShouldReturnFalse() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName.of("foo.bar.baz");
		assertThat(name.isParentOf(grandchild)).isFalse();
		assertThat(grandchild.isParentOf(name)).isFalse();
	}

	@Test
	void isParentOfWhenRootReturnTrue() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("");
		ConfigurationPropertyName child = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.isParentOf(child)).isTrue();
		assertThat(name.isParentOf(grandchild)).isFalse();
		assertThat(child.isAncestorOf(name)).isFalse();
	}

	@Test
	void isAncestorOfWhenSameShouldReturnFalse() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat(name.isAncestorOf(name)).isFalse();
	}

	@Test
	void isAncestorOfWhenParentShouldReturnTrue() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName child = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.isAncestorOf(child)).isTrue();
		assertThat(child.isAncestorOf(name)).isFalse();
	}

	@Test
	void isAncestorOfWhenGrandparentShouldReturnTrue() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName.of("foo.bar.baz");
		assertThat(name.isAncestorOf(grandchild)).isTrue();
		assertThat(grandchild.isAncestorOf(name)).isFalse();
	}

	@Test
	void isAncestorOfWhenRootShouldReturnTrue() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName.of("foo.bar.baz");
		assertThat(name.isAncestorOf(grandchild)).isTrue();
		assertThat(grandchild.isAncestorOf(name)).isFalse();
	}

	@Test
	void compareShouldSortNames() {
		List<ConfigurationPropertyName> names = new ArrayList<>();
		names.add(ConfigurationPropertyName.of("foo[10]"));
		names.add(ConfigurationPropertyName.of("foo.bard"));
		names.add(ConfigurationPropertyName.of("foo[2]"));
		names.add(ConfigurationPropertyName.of("foo.bar"));
		names.add(ConfigurationPropertyName.of("foo.baz"));
		names.add(ConfigurationPropertyName.of("foo"));
		Collections.sort(names);
		assertThat(names.stream().map(ConfigurationPropertyName::toString).collect(Collectors.toList()))
				.containsExactly("foo", "foo[2]", "foo[10]", "foo.bar", "foo.bard", "foo.baz");
	}

	@Test
	void compareDifferentLengthsShouldSortNames() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("spring.resources.chain.strategy.content");
		ConfigurationPropertyName other = ConfigurationPropertyName
				.of("spring.resources.chain.strategy.content.enabled");
		assertThat(name.compareTo(other)).isLessThan(0);
	}

	@Test
	void toStringShouldBeLowerCaseDashed() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("fOO.b_-a-r", '.');
		assertThat(name.toString()).isEqualTo("foo.b-a-r");
	}

	@Test
	void toStringFromOfShouldBeLowerCaseDashed() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.bar-baz");
		assertThat(name.toString()).isEqualTo("foo.bar-baz");
	}

	@Test
	void equalsAndHashCode() {
		ConfigurationPropertyName n01 = ConfigurationPropertyName.of("foo[bar]");
		ConfigurationPropertyName n02 = ConfigurationPropertyName.of("foo[bar]");
		ConfigurationPropertyName n03 = ConfigurationPropertyName.of("foo.bar");
		ConfigurationPropertyName n04 = ConfigurationPropertyName.of("f-o-o.b-a-r");
		ConfigurationPropertyName n05 = ConfigurationPropertyName.of("foo[BAR]");
		ConfigurationPropertyName n06 = ConfigurationPropertyName.of("oof[bar]");
		ConfigurationPropertyName n07 = ConfigurationPropertyName.of("foo.bar");
		ConfigurationPropertyName n08 = ConfigurationPropertyName.EMPTY;
		ConfigurationPropertyName n09 = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName n10 = ConfigurationPropertyName.of("fo");
		ConfigurationPropertyName n11 = ConfigurationPropertyName.adapt("foo.BaR", '.');
		ConfigurationPropertyName n12 = ConfigurationPropertyName.of("f-o-o[b-a-r]");
		ConfigurationPropertyName n13 = ConfigurationPropertyName.of("f-o-o[b-a-r--]");
		ConfigurationPropertyName n14 = ConfigurationPropertyName.of("[1]");
		ConfigurationPropertyName n15 = ConfigurationPropertyName.of("[-1]");
		assertThat(n01.hashCode()).isEqualTo(n02.hashCode());
		assertThat(n01.hashCode()).isEqualTo(n02.hashCode());
		assertThat(n01.hashCode()).isEqualTo(n03.hashCode());
		assertThat(n01.hashCode()).isEqualTo(n04.hashCode());
		assertThat(n01.hashCode()).isEqualTo(n11.hashCode());
		assertThat((Object) n01).isEqualTo(n01);
		assertThat((Object) n01).isEqualTo(n02);
		assertThat((Object) n01).isEqualTo(n03);
		assertThat((Object) n01).isEqualTo(n04);
		assertThat((Object) n11).isEqualTo(n03);
		assertThat((Object) n03).isEqualTo(n11);
		assertThat((Object) n01).isNotEqualTo(n05);
		assertThat((Object) n01).isNotEqualTo(n06);
		assertThat((Object) n07).isNotEqualTo(n08);
		assertThat((Object) n09).isNotEqualTo(n10);
		assertThat((Object) n10).isNotEqualTo(n09);
		assertThat((Object) n12).isNotEqualTo(n13);
		assertThat((Object) n14).isNotEqualTo(n15);
	}

	@Test
	void equalsWhenStartsWith() {
		// gh-14665
		ConfigurationPropertyName n1 = ConfigurationPropertyName.of("my.sources[0].xame");
		ConfigurationPropertyName n2 = ConfigurationPropertyName.of("my.sources[0].xamespace");
		assertThat(n1).isNotEqualTo(n2);
	}

	@Test
	void equalsWhenStartsWithOfAdaptedName() {
		// gh-15152
		ConfigurationPropertyName n1 = ConfigurationPropertyName.adapt("example.mymap.ALPHA", '.');
		ConfigurationPropertyName n2 = ConfigurationPropertyName.adapt("example.mymap.ALPHA_BRAVO", '.');
		assertThat(n1).isNotEqualTo(n2);
	}

	@Test
	void equalsWhenStartsWithOfAdaptedNameOfIllegalChars() {
		// gh-15152
		ConfigurationPropertyName n1 = ConfigurationPropertyName.adapt("example.mymap.ALPH!", '.');
		ConfigurationPropertyName n2 = ConfigurationPropertyName.adapt("example.mymap.ALPHA!BRAVO", '.');
		assertThat(n1).isNotEqualTo(n2);
	}

	@Test
	void equalsWhenNameStartsTheSameUsingDashedCompare() {
		// gh-16855
		ConfigurationPropertyName n1 = ConfigurationPropertyName.of("management.metrics.web.server.auto-time-request");
		ConfigurationPropertyName n2 = ConfigurationPropertyName.of("management.metrics.web.server.auto-time-requests");
		assertThat(n1).isNotEqualTo(n2);
		assertThat(n2).isNotEqualTo(n1);
	}

	@Test
	void isValidWhenValidShouldReturnTrue() {
		assertThat(ConfigurationPropertyName.isValid("")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo.bar")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo[0]")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo[0].baz")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo.b1")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo.b-a-r")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo[FooBar].baz")).isTrue();
	}

	@Test
	void isValidWhenNotValidShouldReturnFalse() {
		assertThat(ConfigurationPropertyName.isValid(null)).isFalse();
		assertThat(ConfigurationPropertyName.isValid("-foo")).isFalse();
		assertThat(ConfigurationPropertyName.isValid("FooBar")).isFalse();
		assertThat(ConfigurationPropertyName.isValid("foo!bar")).isFalse();
	}

	@Test
	void hasIndexedElementWhenHasIndexedElementReturnsTrue() throws Exception {
		assertThat(ConfigurationPropertyName.of("foo[bar]").hasIndexedElement()).isTrue();
	}

	@Test
	void hasIndexedElementWhenHasNoIndexedElementReturnsFalse() throws Exception {
		assertThat(ConfigurationPropertyName.of("foo.bar").hasIndexedElement()).isFalse();
	}

}
