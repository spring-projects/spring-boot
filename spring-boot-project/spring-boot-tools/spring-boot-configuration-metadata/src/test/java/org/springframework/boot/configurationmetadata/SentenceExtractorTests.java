/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.configurationmetadata;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SentenceExtractor}.
 *
 * @author Stephane Nicoll
 */
public class SentenceExtractorTests {

	private static final String NEW_LINE = System.lineSeparator();

	private SentenceExtractor extractor = new SentenceExtractor();

	@Test
	public void extractFirstSentence() {
		String sentence = this.extractor.getFirstSentence("My short " + "description. More stuff.");
		assertThat(sentence).isEqualTo("My short description.");
	}

	@Test
	public void extractFirstSentenceNewLineBeforeDot() {
		String sentence = this.extractor
				.getFirstSentence("My short" + NEW_LINE + "description." + NEW_LINE + "More stuff.");
		assertThat(sentence).isEqualTo("My short description.");
	}

	@Test
	public void extractFirstSentenceNewLineBeforeDotWithSpaces() {
		String sentence = this.extractor
				.getFirstSentence("My short  " + NEW_LINE + " description.  " + NEW_LINE + "More stuff.");
		assertThat(sentence).isEqualTo("My short description.");
	}

	@Test
	public void extractFirstSentenceNoDot() {
		String sentence = this.extractor.getFirstSentence("My short description");
		assertThat(sentence).isEqualTo("My short description");
	}

	@Test
	public void extractFirstSentenceNoDotMultipleLines() {
		String sentence = this.extractor.getFirstSentence("My short description " + NEW_LINE + " More stuff");
		assertThat(sentence).isEqualTo("My short description");
	}

	@Test
	public void extractFirstSentenceNull() {
		assertThat(this.extractor.getFirstSentence(null)).isNull();
	}

}
