/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.test.autoconfigure;

import java.io.File;
import java.util.List;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Metadata describing a module's test slices.
 *
 * @param module the module's name
 * @param testSlices the module's test slices
 * @author Andy Wilkinson
 */
record TestSliceMetadata(String module, List<TestSlice> testSlices) {

	static TestSliceMetadata readFrom(File file) {
		return JsonMapper.builder().build().readValue(file, TestSliceMetadata.class);
	}

	void writeTo(File file) {
		JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build().writeValue(file, this);
	}

	record TestSlice(String annotation, List<String> importedAutoConfigurations) {

	}

}
