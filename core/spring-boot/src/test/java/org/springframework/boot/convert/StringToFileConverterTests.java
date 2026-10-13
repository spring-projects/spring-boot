/*
 * Copyright 2012-present the original author or authors.
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
