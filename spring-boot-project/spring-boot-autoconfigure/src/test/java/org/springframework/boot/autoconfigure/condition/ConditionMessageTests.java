/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.List;

import org.junit.Test;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionMessage}.
 *
 * @author Phillip Webb
 */
public class ConditionMessageTests {

	@Test
	public void isEmptyWhenEmptyShouldReturnTrue() {
		ConditionMessage message = ConditionMessage.empty();
		assertThat(message.isEmpty()).isTrue();
	}

	@Test
	public void isEmptyWhenNotEmptyShouldReturnFalse() {
		ConditionMessage message = ConditionMessage.of("Test");
		assertThat(message.isEmpty()).isFalse();
	}

	@Test
	public void toStringWhenEmptyShouldReturnEmptyString() {
		ConditionMessage message = ConditionMessage.empty();
		assertThat(message.toString()).isEqualTo("");
	}

	@Test
	public void toStringWhenHasMessageShouldReturnMessage() {
		ConditionMessage message = ConditionMessage.of("Test");
		assertThat(message.toString()).isEqualTo("Test");
	}

	@Test
	public void appendWhenHasExistingMessageShouldAddSpace() {
		ConditionMessage message = ConditionMessage.of("a").append("b");
		assertThat(message.toString()).isEqualTo("a b");
	}

	@Test
	public void appendWhenAppendingNullShouldDoNothing() {
		ConditionMessage message = ConditionMessage.of("a").append(null);
		assertThat(message.toString()).isEqualTo("a");
	}

	@Test
	public void appendWhenNoMessageShouldNotAddSpace() {
		ConditionMessage message = ConditionMessage.empty().append("b");
		assertThat(message.toString()).isEqualTo("b");
	}

	@Test
	public void andConditionWhenUsingClassShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.empty().andCondition(Test.class)
				.because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	public void andConditionWhenUsingStringShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.empty().andCondition("@Test")
				.because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	public void andConditionWhenIncludingDetailsShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.empty()
				.andCondition(Test.class, "(a=b)").because("OK");
		assertThat(message.toString()).isEqualTo("@Test (a=b) OK");
	}

	@Test
	public void ofCollectionShouldCombine() {
		List<ConditionMessage> messages = new ArrayList<>();
		messages.add(ConditionMessage.of("a"));
		messages.add(ConditionMessage.of("b"));
		ConditionMessage message = ConditionMessage.of(messages);
		assertThat(message.toString()).isEqualTo("a; b");
	}

	@Test
	public void ofCollectionWhenNullShouldReturnEmpty() {
		ConditionMessage message = ConditionMessage.of((List<ConditionMessage>) null);
		assertThat(message.isEmpty()).isTrue();
	}

	@Test
	public void forConditionShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.forCondition("@Test").because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	public void forConditionShouldNotAddExtraSpaceWithEmptyCondition() {
		ConditionMessage message = ConditionMessage.forCondition("").because("OK");
		assertThat(message.toString()).isEqualTo("OK");
	}

	@Test
	public void forConditionWhenClassShouldIncludeCondition() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class, "(a=b)")
				.because("OK");
		assertThat(message.toString()).isEqualTo("@Test (a=b) OK");
	}

	@Test
	public void foundExactlyShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.foundExactly("abc");
		assertThat(message.toString()).isEqualTo("@Test found abc");
	}

	@Test
	public void foundWhenSingleElementShouldUseSingular() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.found("bean", "beans").items("a");
		assertThat(message.toString()).isEqualTo("@Test found bean a");
	}

	@Test
	public void foundNoneAtAllShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.found("no beans").atAll();
		assertThat(message.toString()).isEqualTo("@Test found no beans");
	}

	@Test
	public void foundWhenMultipleElementsShouldUsePlural() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.found("bean", "beans").items("a", "b", "c");
		assertThat(message.toString()).isEqualTo("@Test found beans a, b, c");
	}

	@Test
	public void foundWhenQuoteStyleShouldQuote() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.found("bean", "beans").items(Style.QUOTE, "a", "b", "c");
		assertThat(message.toString()).isEqualTo("@Test found beans 'a', 'b', 'c'");
	}

	@Test
	public void didNotFindWhenSingleElementShouldUseSingular() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.didNotFind("class", "classes").items("a");
		assertThat(message.toString()).isEqualTo("@Test did not find class a");
	}

	@Test
	public void didNotFindWhenMultipleElementsShouldUsePlural() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.didNotFind("class", "classes").items("a", "b", "c");
		assertThat(message.toString()).isEqualTo("@Test did not find classes a, b, c");
	}

	@Test
	public void resultedInShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.resultedIn("Green");
		assertThat(message.toString()).isEqualTo("@Test resulted in Green");
	}

	@Test
	public void notAvailableShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.notAvailable("JMX");
		assertThat(message.toString()).isEqualTo("@Test JMX is not available");
	}

	@Test
	public void availableShouldConstructMessage() {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.available("JMX");
		assertThat(message.toString()).isEqualTo("@Test JMX is available");
	}

}
