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

package org.springframework.boot.convert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.boot.DigitalAmount;
import org.springframework.boot.DigitalAmountStyle;
import org.springframework.boot.DigitalUnit;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToDigitalAmountConverter}.
 *
 * @author Dmytro Nosan
 */
@RunWith(Parameterized.class)
public class StringToDigitalAmountConverterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final ConversionService conversionService;

	public StringToDigitalAmountConverterTests(String name,
			ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertWhenSimpleBytesShouldReturnDigitalAmount() {
		assertThat(convert("10b")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(convert("10B")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(convert("+10 B")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(convert("-10 b")).isEqualTo(DigitalAmount.fromBytes(-10));
	}

	@Test
	public void convertWhenSimpleKilobytesShouldReturnDigitalAmount() {
		assertThat(convert("10kb")).isEqualTo(DigitalAmount.fromKilobytes(10));
		assertThat(convert("10Kb")).isEqualTo(DigitalAmount.fromKilobytes(10));
		assertThat(convert("+10KB")).isEqualTo(DigitalAmount.fromKilobytes(10));
		assertThat(convert("-10 kb")).isEqualTo(DigitalAmount.fromKilobytes(-10));
	}

	@Test
	public void convertWhenSimpleMegabytesShouldReturnDigitalAmount() {
		assertThat(convert("10MB")).isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(convert("10 mb")).isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(convert("+10 Mb")).isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(convert("-10 mB")).isEqualTo(DigitalAmount.fromMegabytes(-10));
	}

	@Test
	public void convertWhenSimpleGigabytesShouldReturnDigitalAmount() {
		assertThat(convert("10GB")).isEqualTo(DigitalAmount.fromGigabytes(10));
		assertThat(convert("10 gb")).isEqualTo(DigitalAmount.fromGigabytes(10));
		assertThat(convert("+10 Gb")).isEqualTo(DigitalAmount.fromGigabytes(10));
		assertThat(convert("-10 gB")).isEqualTo(DigitalAmount.fromGigabytes(-10));
	}

	@Test
	public void convertWhenSimpleTerabytesShouldReturnDigitalAmount() {
		assertThat(convert("10TB")).isEqualTo(DigitalAmount.fromTerabytes(10));
		assertThat(convert("10 tb")).isEqualTo(DigitalAmount.fromTerabytes(10));
		assertThat(convert("+10 Tb")).isEqualTo(DigitalAmount.fromTerabytes(10));
		assertThat(convert("-10 tB")).isEqualTo(DigitalAmount.fromTerabytes(-10));
	}

	@Test
	public void convertWhenSimpleWithoutSuffixShouldReturnDigitalAmount() {
		assertThat(convert("10")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(convert("+10")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(convert("-10")).isEqualTo(DigitalAmount.fromBytes(-10));
	}

	@Test
	public void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDigitalAmount() {
		assertThat(convert("10", DigitalUnit.MEGABYTES, null))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(convert("+10", DigitalUnit.MEGABYTES, null))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(convert("-10", DigitalUnit.MEGABYTES, null))
				.isEqualTo(DigitalAmount.fromMegabytes(-10));
	}

	@Test
	public void convertWhenBadFormatShouldThrowException() {
		this.thrown.expect(ConversionFailedException.class);
		this.thrown.expectMessage("'10foo' is not a valid digital");
		convert("10foo");
	}

	@Test
	public void convertWhenStyleMismatchShouldThrowException() {
		this.thrown.expect(ConversionFailedException.class);
		convert("10000qeq ", null, DigitalAmountStyle.SIMPLE);
	}

	@Test
	public void convertWhenEmptyShouldReturnNull() {
		assertThat(convert("")).isNull();
	}

	private DigitalAmount convert(String source) {
		return this.conversionService.convert(source, DigitalAmount.class);
	}

	private DigitalAmount convert(String source, DigitalUnit unit,
			DigitalAmountStyle style) {
		return (DigitalAmount) this.conversionService.convert(source,
				TypeDescriptor.forObject(source),
				MockDigitalAmountTypeDescriptor.get(unit, style));
	}

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new StringToDigitalAmountConverter());
	}

}
