/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.convert;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToFileConverter}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class StringToFileConverterTests {

	@TempDir
	@SuppressWarnings("NullAway.Init")
	File temp;

	@ConversionServiceTest
	void convertWhenSimpleFileReturnsFile(ConversionService conversionService) {
		assertThat(convert(conversionService, this.temp.getAbsolutePath() + "/test"))
			.isEqualTo(new File(this.temp, "test").getAbsoluteFile());
	}

	@ConversionServiceTest
	void convertWhenFilePrefixedReturnsFile(ConversionService conversionService) {
		File file = convert(conversionService, "file:" + this.temp.getAbsolutePath() + "/test");
		assertThat(file).isNotNull();
		assertThat(file.getAbsoluteFile()).isEqualTo(new File(this.temp, "test").getAbsoluteFile());
	}

	@ConversionServiceTest
	@WithResource(name = "com/example/test-file.txt", content = "test content")
	void convertWhenClasspathPrefixedReturnsFile(ConversionService conversionService) throws IOException {
		String resource = new ClassPathResource("com/example/test-file.txt").getURL().getFile();
		File file = convert(conversionService, "classpath:com/example/test-file.txt");
		assertThat(file).isNotNull();
		assertThat(file.getAbsoluteFile()).isEqualTo(new File(resource).getAbsoluteFile());
	}

	private @Nullable File convert(ConversionService conversionService, String source) {
		return conversionService.convert(source, File.class);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
			.with((conversionService) -> conversionService.addConverter(new StringToFileConverter()));
	}

}
