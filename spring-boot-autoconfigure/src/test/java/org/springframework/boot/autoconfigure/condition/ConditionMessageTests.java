/*
 * Copyright 2012-2017 the original author or authors.
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
	public void isEmptyWhenEmptyShouldReturnTrue() throws Exception {
		ConditionMessage message = ConditionMessage.empty();
		assertThat(message.isEmpty()).isTrue();
	}

	@Test
	public void isEmptyWhenNotEmptyShouldReturnFalse() throws Exception {
		ConditionMessage message = ConditionMessage.of("Test");
		assertThat(message.isEmpty()).isFalse();
	}

	@Test
	public void toStringWhenEmptyShouldReturnEmptyString() throws Exception {
		ConditionMessage message = ConditionMessage.empty();
		assertThat(message.toString()).isEqualTo("");
	}

	@Test
	public void toStringWhenHasMessageShouldReturnMessage() throws Exception {
		ConditionMessage message = ConditionMessage.of("Test");
		assertThat(message.toString()).isEqualTo("Test");
	}

	@Test
	public void appendWhenHasExistingMessageShouldAddSpace() throws Exception {
		ConditionMessage message = ConditionMessage.of("a").append("b");
		assertThat(message.toString()).isEqualTo("a b");
	}

	@Test
	public void appendWhenAppendingNullShouldDoNothing() throws Exception {
		ConditionMessage message = ConditionMessage.of("a").append(null);
		assertThat(message.toString()).isEqualTo("a");
	}

	@Test
	public void appendWhenNoMessageShouldNotAddSpace() throws Exception {
		ConditionMessage message = ConditionMessage.empty().append("b");
		assertThat(message.toString()).isEqualTo("b");
	}

	@Test
	public void andConditionWhenUsingClassShouldIncludeCondition() throws Exception {
		ConditionMessage message = ConditionMessage.empty().andCondition(Test.class)
				.because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	public void andConditionWhenUsingStringShouldIncludeCondition() throws Exception {
		ConditionMessage message = ConditionMessage.empty().andCondition("@Test")
				.because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	public void andConditionWhenIncludingDetailsShouldIncludeCondition()
			throws Exception {
		ConditionMessage message = ConditionMessage.empty()
				.andCondition(Test.class, "(a=b)").because("OK");
		assertThat(message.toString()).isEqualTo("@Test (a=b) OK");
	}

	@Test
	public void ofCollectionShouldCombine() throws Exception {
		List<ConditionMessage> messages = new ArrayList<ConditionMessage>();
		messages.add(ConditionMessage.of("a"));
		messages.add(ConditionMessage.of("b"));
		ConditionMessage message = ConditionMessage.of(messages);
		assertThat(message.toString()).isEqualTo("a; b");
	}

	@Test
	public void ofCollectionWhenNullShouldReturnEmpty() throws Exception {
		ConditionMessage message = ConditionMessage.of((List<ConditionMessage>) null);
		assertThat(message.isEmpty()).isTrue();
	}

	@Test
	public void forConditionShouldIncludeCondition() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition("@Test").because("OK");
		assertThat(message.toString()).isEqualTo("@Test OK");
	}

	@Test
	public void forConditionShouldNotAddExtraSpaceWithEmptyCondition() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition("").because("OK");
		assertThat(message.toString()).isEqualTo("OK");
	}

	@Test
	public void forConditionWhenClassShouldIncludeCondition() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class, "(a=b)")
				.because("OK");
		assertThat(message.toString()).isEqualTo("@Test (a=b) OK");
	}

	@Test
	public void foundExactlyShouldConstructMessage() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.foundExactly("abc");
		assertThat(message.toString()).isEqualTo("@Test found abc");
	}

	@Test
	public void foundWhenSingleElementShouldUseSingular() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.found("bean", "beans").items("a");
		assertThat(message.toString()).isEqualTo("@Test found bean a");
	}

	@Test
	public void foundNoneAtAllShouldConstructMessage() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.found("no beans").atAll();
		assertThat(message.toString()).isEqualTo("@Test found no beans");
	}

	@Test
	public void foundWhenMultipleElementsShouldUsePlural() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.found("bean", "beans").items("a", "b", "c");
		assertThat(message.toString()).isEqualTo("@Test found beans a, b, c");
	}

	@Test
	public void foundWhenQuoteStyleShouldQuote() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.found("bean", "beans").items(Style.QUOTE, "a", "b", "c");
		assertThat(message.toString()).isEqualTo("@Test found beans 'a', 'b', 'c'");
	}

	@Test
	public void didNotFindWhenSingleElementShouldUseSingular() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.didNotFind("class", "classes").items("a");
		assertThat(message.toString()).isEqualTo("@Test did not find class a");
	}

	@Test
	public void didNotFindWhenMultipleElementsShouldUsePlural() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.didNotFind("class", "classes").items("a", "b", "c");
		assertThat(message.toString()).isEqualTo("@Test did not find classes a, b, c");
	}

	@Test
	public void resultedInShouldConstructMessage() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.resultedIn("Green");
		assertThat(message.toString()).isEqualTo("@Test resulted in Green");
	}

	@Test
	public void notAvailableShouldConstructMessage() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.notAvailable("JMX");
		assertThat(message.toString()).isEqualTo("@Test JMX is not available");
	}

	@Test
	public void availableShouldConstructMessage() throws Exception {
		ConditionMessage message = ConditionMessage.forCondition(Test.class)
				.available("JMX");
		assertThat(message.toString()).isEqualTo("@Test JMX is available");
	}

}
