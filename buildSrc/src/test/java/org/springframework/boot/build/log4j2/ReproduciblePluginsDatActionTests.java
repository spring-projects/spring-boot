/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.boot.build.log4j2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.logging.log4j.core.config.plugins.processor.PluginCache;
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReproducibleLog4j2PluginsDatAction}
 *
 * @author Andy Wilkinson
 */
public class ReproduciblePluginsDatActionTests {

	@Test
	void postProcessingOrdersCategoriesAndPlugins() throws IOException {
		Path datFile = Files.createTempFile("Log4j2Plugins", "dat");
		try {
			write(datFile);
			PluginCache cache = new PluginCache();
			cache.loadCacheFiles(new Vector<>(Arrays.asList(datFile.toUri().toURL())).elements());
			assertThat(cache.getAllCategories().keySet()).containsExactly("one", "two");
			assertThat(cache.getCategory("one").keySet()).containsExactly("alpha", "bravo", "charlie");
			assertThat(cache.getCategory("two").keySet()).containsExactly("delta", "echo", "foxtrot");
		}
		finally {
			Files.delete(datFile);
		}
	}

	private void write(Path datFile) throws IOException {
		PluginCache cache = new PluginCache();
		createCategory(cache, "two", Arrays.asList("delta", "foxtrot", "echo"));
		createCategory(cache, "one", Arrays.asList("bravo", "alpha", "charlie"));
		try (OutputStream output = new FileOutputStream(datFile.toFile())) {
			cache.writeCache(output);
			new ReproducibleLog4j2PluginsDatAction().postProcess(datFile.toFile());
		}
	}

	private void createCategory(PluginCache cache, String categoryName, List<String> entryNames) {
		Map<String, PluginEntry> category = cache.getCategory(categoryName);
		for (String entryName : entryNames) {
			PluginEntry entry = new PluginEntry();
			entry.setKey(entryName);
			entry.setClassName("com.example.Plugin");
			entry.setName("name");
			entry.setCategory(categoryName);
			category.put(entryName, entry);
		}
	}

}
