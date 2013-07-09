/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.launcher;

import java.util.List;

/**
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /lib} directory.
 * 
 * @author Phillip Webb
 */
public class JarLauncher extends Launcher {

	@Override
	protected boolean isNestedArchive(Archive.Entry entry) {
		return !entry.isDirectory() && entry.getName().startsWith("lib/");
	}

	@Override
	protected void postProcessLib(Archive archive, List<Archive> lib) throws Exception {
		lib.add(0, archive);
	}

	public static void main(String[] args) {
		new JarLauncher().launch(args);
	}

}
