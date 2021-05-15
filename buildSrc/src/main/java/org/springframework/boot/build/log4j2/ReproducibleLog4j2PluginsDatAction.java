/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * An {@link Action} to post-process a {@code Log4j2Plugins.dat} and re-order its content
 * so that it is reproducible.
 *
 * @author Andy Wilkinson
 */
public class ReproducibleLog4j2PluginsDatAction implements Action<JavaCompile> {

	@Override
	public void execute(JavaCompile javaCompile) {
		File datFile = new File(javaCompile.getDestinationDir(),
				"META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat");
		try {
			postProcess(datFile);
		}
		catch (IOException ex) {
			throw new TaskExecutionException(javaCompile, ex);
		}
	}

	void postProcess(File datFile) throws IOException {
		if (!datFile.isFile()) {
			throw new InvalidUserDataException(
					"META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat not found");
		}
		Map<String, Map<String, Plugin>> categories = new TreeMap<>();
		try (DataInputStream input = new DataInputStream(new FileInputStream(datFile))) {
			int categoryCount = input.readInt();
			for (int i = 0; i < categoryCount; i++) {
				String categoryName = input.readUTF();
				int pluginCount = input.readInt();
				Map<String, Plugin> category = categories.computeIfAbsent(categoryName, (c) -> new TreeMap<>());
				for (int j = 0; j < pluginCount; j++) {
					Plugin plugin = new Plugin(input.readUTF(), input.readUTF(), input.readUTF(), input.readBoolean(),
							input.readBoolean());
					category.putIfAbsent(plugin.key, plugin);
				}
			}
		}
		try (DataOutputStream output = new DataOutputStream(new FileOutputStream(datFile))) {
			output.writeInt(categories.size());
			for (Entry<String, Map<String, Plugin>> category : categories.entrySet()) {
				output.writeUTF(category.getKey());
				output.writeInt(category.getValue().size());
				for (Plugin plugin : category.getValue().values()) {
					output.writeUTF(plugin.key);
					output.writeUTF(plugin.className);
					output.writeUTF(plugin.name);
					output.writeBoolean(plugin.printable);
					output.writeBoolean(plugin.defer);
				}
			}
		}
	}

	private static final class Plugin {

		private final String key;

		private final String className;

		private final String name;

		private final boolean printable;

		private final boolean defer;

		private Plugin(String key, String className, String name, boolean printable, boolean defer) {
			this.key = key;
			this.className = className;
			this.name = name;
			this.printable = printable;
			this.defer = defer;
		}

	}

}
