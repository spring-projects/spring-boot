/*
 * Copyright 2012-2016 the original author or authors.
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
 * Tests for {@link DescriptionExtractor}.
 *
 * @author Stephane Nicoll
 */
public class DescriptionExtractorTests {

	private static final String NEW_LINE = System.getProperty("line.separator");

	private DescriptionExtractor extractor = new DescriptionExtractor();

	@Test
	public void extractShortDescription() {
		String description = this.extractor
				.getShortDescription("My short " + "description. More stuff.");
		assertThat(description).isEqualTo("My short description.");
	}

	@Test
	public void extractShortDescriptionNewLineBeforeDot() {
		String description = this.extractor.getShortDescription(
				"My short" + NEW_LINE + "description." + NEW_LINE + "More stuff.");
		assertThat(description).isEqualTo("My short description.");
	}

	@Test
	public void extractShortDescriptionNewLineBeforeDotWithSpaces() {
		String description = this.extractor.getShortDescription(
				"My short  " + NEW_LINE + " description.  " + NEW_LINE + "More stuff.");
		assertThat(description).isEqualTo("My short description.");
	}

	@Test
	public void extractShortDescriptionNoDot() {
		String description = this.extractor.getShortDescription("My short description");
		assertThat(description).isEqualTo("My short description");
	}

	@Test
	public void extractShortDescriptionNoDotMultipleLines() {
		String description = this.extractor
				.getShortDescription("My short description " + NEW_LINE + " More stuff");
		assertThat(description).isEqualTo("My short description");
	}

	@Test
	public void extractShortDescriptionNull() {
		assertThat(this.extractor.getShortDescription(null)).isEqualTo(null);
	}

}
