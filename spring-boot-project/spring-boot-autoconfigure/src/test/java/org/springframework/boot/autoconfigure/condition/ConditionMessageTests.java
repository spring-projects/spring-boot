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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionMessage}.
 *
 * @author Phillip Webb
 */
class ConditionMessageTests {

	@Test
	void isEmptyWhenEmptyShouldReturnTrue() {
		ConditionMessage message = ConditionMessage.empty();
		assertThat(message.isEmpty()).isTrue();
	}

	@Test
	void isEmptyWhenNotEmptyShouldReturnFalse() {
		ConditionMessage message = ConditionMessage.of("Test");
		assertThat(message.isEmpty()).isFalse();
	}

	@Test
	void toStringWhenEmptyShouldReturnEmptyString() {
		ConditionMessage message = ConditionMessage.empty();
		assertThat(message.toString()).isEqualTo("");
	}

	@Test
	void toStringWhenHasMessageShouldReturnMessage() {
		ConditionMessage message = ConditionMessage.of("Test");
		assertThat(message.toString()).isEqualTo("Test");
	}

	@Test
	void appendWhenHasExistingMessageShouldAddSpace() {
		ConditionMessage message = ConditionMessage.of("a").append("b");
		assertThat(message.toString()).isEqualTo("a b");
	}

	@Test
	void appendWhenAppendingNullShouldDoNothing() {
		ConditionMessage message = ConditionMessage.of("a").append(null);
		assertThat(message.toString()).isEqualTo("a");
	}

	@Test
	void appendWhenNoMessageShouldNotAddSpace() {
		ConditionMessage message = ConditionMessage.empty().append("b");
		assertThat(message.toString()).isEqualTo("b");
	}

	@Test
	void andConditionWhenUsingClassShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.empty().andCondition(Test.class).because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	void andConditionWhenUsingStringShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.empty().andCondition("@Test").because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	void andConditionWhenIncludingDetailsShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.empty().andCondition(Test.class, "(a=b)").because("OK");
		assertThat(message.toString()).isEqualTo("@Test (a=b) OK");
	}

	@Test
	void ofCollectionShouldCombine() {
		List<ConditionMessage> messages = new ArrayList<>();
		messages.add(ConditionMessage.of("a"));
		messages.add(ConditionMessage.of("b"));
		ConditionMessage message = ConditionMessage.of(messages);
		assertThat(message.toString()).isEqualTo("a; b");
	}

	@Test
	void ofCollectionWhenNullShouldReturnEmpty() {
		ConditionMessage message = ConditionMessage.of((List<ConditionMessage>) null);
		assertThat(message.isEmpty()).isTrue();
	}

	@Test
	void forConditionShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.forCondition("@Test").because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	void forConditionShouldNotAddExtraSpaceWithEmptyCondition() {
		ConditionMessage message = ConditionMessage.forCondition("").because("OK");
		assertThat(message.toString()).isEqualTo("OK");
	}

	@Test
	void forConditionWhenClassShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class, "(a=b)").because("OK");
		assertThat(message.toString()).isEqualTo("@Test (a=b) OK");
	}

	@Test
	void foundExactlyShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).foundExactly("abc");
		assertThat(message.toString()).isEqualTo("@Test found abc");
	}

	@Test
	void foundWhenSingleElementShouldUseSingular() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).found("bean", "beans").items("a");
		assertThat(message.toString()).isEqualTo("@Test found bean a");
	}

	@Test
	void foundNoneAtAllShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).found("no beans").atAll();
		assertThat(message.toString()).isEqualTo("@Test found no beans");
	}

	@Test
	void foundWhenMultipleElementsShouldUsePlural() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).found("bean", "beans").items("a", "b",
				"c");
		assertThat(message.toString()).isEqualTo("@Test found beans a, b, c");
	}

	@Test
	void foundWhenQuoteStyleShouldQuote() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).found("bean", "beans").items(Style.QUOTE,
				"a", "b", "c");
		assertThat(message.toString()).isEqualTo("@Test found beans 'a', 'b', 'c'");
	}

	@Test
	void didNotFindWhenSingleElementShouldUseSingular() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).didNotFind("class", "classes").items("a");
		assertThat(message.toString()).isEqualTo("@Test did not find class a");
	}

	@Test
	void didNotFindWhenMultipleElementsShouldUsePlural() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).didNotFind("class", "classes").items("a",
				"b", "c");
		assertThat(message.toString()).isEqualTo("@Test did not find classes a, b, c");
	}

	@Test
	void resultedInShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).resultedIn("Green");
		assertThat(message.toString()).isEqualTo("@Test resulted in Green");
	}

	@Test
	void notAvailableShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).notAvailable("JMX");
		assertThat(message.toString()).isEqualTo("@Test JMX is not available");
	}

	@Test
	void availableShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class).available("JMX");
		assertThat(message.toString()).isEqualTo("@Test JMX is available");
	}

	@Test
	void itemsTolerateNullInput() {
		Collection<?> items = null;
		ConditionMessage message = ConditionMessage.forCondition(Test.class).didNotFind("item").items(items);
		assertThat(message.toString()).isEqualTo("@Test did not find item");
	}

	@Test
	void quotedItemsTolerateNullInput() {
		Collection<?> items = null;
		ConditionMessage message = ConditionMessage.forCondition(Test.class).didNotFind("item").items(Style.QUOTE,
				items);
		assertThat(message.toString()).isEqualTo("@Test did not find item");
	}

}
