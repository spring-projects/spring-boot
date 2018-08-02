/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link DigitalAmountStyle}.
 *
 * @author Dmytro Nosan
 */
public class DigitalAmountStyleTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void detectAndParseWhenValueIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Digital Amount pattern must not be null");
		detectAndParse(null, null);
	}

	@Test
	public void detectAndParseWhenSimpleBytesDigitalAmount() {
		assertThat(detectAndParse("10b")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(detectAndParse("10B")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(detectAndParse("+10 B")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(detectAndParse("-10 b")).isEqualTo(DigitalAmount.fromBytes(-10));
	}

	@Test
	public void detectAndParseWhenSimpleKilobytesShouldReturnDigitalAmount() {
		assertThat(detectAndParse("10kb")).isEqualTo(DigitalAmount.fromKilobytes(10));
		assertThat(detectAndParse("10KB")).isEqualTo(DigitalAmount.fromKilobytes(10));
		assertThat(detectAndParse("+10 KB")).isEqualTo(DigitalAmount.fromKilobytes(10));
		assertThat(detectAndParse("-10 kb")).isEqualTo(DigitalAmount.fromKilobytes(-10));
	}

	@Test
	public void detectAndParseWhenSimpleMegabytesShouldReturnDigitalAmount() {
		assertThat(detectAndParse("10mb")).isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(detectAndParse("10MB")).isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(detectAndParse("+10 MB")).isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(detectAndParse("-10 mb")).isEqualTo(DigitalAmount.fromMegabytes(-10));
	}

	@Test
	public void detectAndParseWhenSimpleGigabytesShouldReturnDigitalAmount() {
		assertThat(detectAndParse("10gb")).isEqualTo(DigitalAmount.fromGigabytes(10));
		assertThat(detectAndParse("10GB")).isEqualTo(DigitalAmount.fromGigabytes(10));
		assertThat(detectAndParse("+10 gb")).isEqualTo(DigitalAmount.fromGigabytes(10));
		assertThat(detectAndParse("-10 GB")).isEqualTo(DigitalAmount.fromGigabytes(-10));
	}

	@Test
	public void detectAndParseWhenSimpleTerabytesShouldReturnDigitalAmount() {
		assertThat(detectAndParse("10tb")).isEqualTo(DigitalAmount.fromTerabytes(10));
		assertThat(detectAndParse("10TB")).isEqualTo(DigitalAmount.fromTerabytes(10));
		assertThat(detectAndParse("+10 tb")).isEqualTo(DigitalAmount.fromTerabytes(10));
		assertThat(detectAndParse("-10 TB")).isEqualTo(DigitalAmount.fromTerabytes(-10));
	}

	@Test
	public void detectAndParseWhenSimpleWithoutSuffixShouldReturnDigitalAmount() {
		assertThat(detectAndParse("10")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(detectAndParse("+10")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(detectAndParse("-10")).isEqualTo(DigitalAmount.fromBytes(-10));
	}

	@Test
	public void detectAndParseWhenSimpleWithoutSuffixButWithDigitalUnitShouldReturnDigitalAmount() {
		assertThat(detectAndParse("10", DigitalUnit.MEGABYTES))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(detectAndParse("+10", DigitalUnit.MEGABYTES))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(detectAndParse("-10", DigitalUnit.MEGABYTES))
				.isEqualTo(DigitalAmount.fromMegabytes(-10));
	}

	@Test
	public void detectAndParseWhenBadFormatShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("'10foo' is not a valid digital amount");
		detectAndParse("10foo");
	}

	@Test
	public void detectWhenUnknownShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("'bad' is not a valid digital amount");
		DigitalAmountStyle.detect("bad");
	}

	@Test
	public void parseSimpleShouldParse() {
		assertThat(DigitalAmountStyle.SIMPLE.parse("10mb"))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
	}

	@Test
	public void parseSimpleWithUnitShouldUseUnitAsFallback() {
		assertThat(DigitalAmountStyle.SIMPLE.parse("10mb", DigitalUnit.KILOBYTES))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(DigitalAmountStyle.SIMPLE.parse("10", DigitalUnit.KILOBYTES))
				.isEqualTo(DigitalAmount.fromKilobytes(10));
	}

	@Test
	public void parseSimpleWhenUnknownUnitShouldThrowException() {
		try {
			DigitalAmountStyle.SIMPLE.parse("10EB");
			fail("Did not throw");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getCause().getMessage()).isEqualTo("Unknown abbreviation 'EB'");
		}
	}

	@Test
	public void printSimpleWithoutUnitShouldPrintInBytes() {
		assertThat(DigitalAmountStyle.SIMPLE.print(DigitalAmount.fromKilobytes(1)))
				.isEqualTo("1024B");
	}

	@Test
	public void printSimpleWithUnitShouldPrintInUnit() {
		assertThat(DigitalAmountStyle.SIMPLE.print(DigitalAmount.fromBytes(1024),
				DigitalUnit.KILOBYTES)).isEqualTo("1KB");
	}

	private static DigitalAmount detectAndParse(CharSequence value, DigitalUnit unit) {
		return DigitalAmountStyle.detect(value).parse(value, unit);
	}

	private static DigitalAmount detectAndParse(CharSequence value) {
		return detectAndParse(value, null);
	}

}
