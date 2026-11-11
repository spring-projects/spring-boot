/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.UnaryOperator;

import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * Public version of {@link AutoConfigurationSorter} for use in tests.
 *
 * @author Phillip Webb
 */
public class TestAutoConfigurationSorter extends AutoConfigurationSorter {

	public TestAutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
			UnaryOperator<String> replacementMapper) {
		super(metadataReaderFactory, AutoConfigurationMetadataLoader.loadMetadata(new Properties()), replacementMapper);
	}

	@Override
	public List<String> getInPriorityOrder(Collection<String> classNames) {
		return super.getInPriorityOrder(classNames);
	}

}
