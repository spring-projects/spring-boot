/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Properties;

import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * Public version of {@link AutoConfigurationSorter} for use in tests.
 *
 * @author Phillip Webb
 */
public class TestAutoConfigurationSorter extends AutoConfigurationSorter {

	public TestAutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory) {
		super(metadataReaderFactory,
				AutoConfigurationMetadataLoader.loadMetadata(new Properties()));
	}

}
