/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SentenceExtractor}.
 *
 * @author Stephane Nicoll
 */
class SentenceExtractorTests {

	private static final String NEW_LINE = System.lineSeparator();

	private final SentenceExtractor extractor = new SentenceExtractor();

	@Test
	void extractFirstSentence() {
		String sentence = this.extractor.getFirstSentence("My short description. More stuff.");
		assertThat(sentence).isEqualTo("My short description.");
	}

	@Test
	void extractFirstSentenceNewLineBeforeDot() {
		String sentence = this.extractor
			.getFirstSentence("My short" + NEW_LINE + "description." + NEW_LINE + "More stuff.");
		assertThat(sentence).isEqualTo("My short description.");
	}

	@Test
	void extractFirstSentenceNewLineBeforeDotWithSpaces() {
		String sentence = this.extractor
			.getFirstSentence("My short  " + NEW_LINE + " description.  " + NEW_LINE + "More stuff.");
		assertThat(sentence).isEqualTo("My short description.");
	}

	@Test
	void extractFirstSentenceNoDot() {
		String sentence = this.extractor.getFirstSentence("My short description");
		assertThat(sentence).isEqualTo("My short description");
	}

	@Test
	void extractFirstSentenceNoDotMultipleLines() {
		String sentence = this.extractor.getFirstSentence("My short description " + NEW_LINE + " More stuff");
		assertThat(sentence).isEqualTo("My short description");
	}

	@Test
	void extractFirstSentenceNull() {
		assertThat(this.extractor.getFirstSentence(null)).isNull();
	}

}
