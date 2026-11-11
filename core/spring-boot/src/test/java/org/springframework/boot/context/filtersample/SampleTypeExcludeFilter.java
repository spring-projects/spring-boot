/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.filtersample;

import java.io.IOException;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

public class SampleTypeExcludeFilter extends TypeExcludeFilter {

	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		return metadataReader.getClassMetadata().getClassName().equals(ExampleFilteredComponent.class.getName());
	}

}
