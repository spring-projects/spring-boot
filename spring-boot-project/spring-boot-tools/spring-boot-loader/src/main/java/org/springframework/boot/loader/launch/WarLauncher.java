/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.launch;

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.2.0
 */
public class WarLauncher extends ExecutableArchiveLauncher {

	public WarLauncher() throws Exception {
	}

	protected WarLauncher(Archive archive) throws Exception {
		super(archive);
	}

	@Override
	public boolean isIncludedOnClassPath(Archive.Entry entry) {
		String name = entry.name();
		if (entry.isDirectory()) {
			return name.equals("WEB-INF/classes/");
		}
		return name.startsWith("WEB-INF/lib/") || name.startsWith("WEB-INF/lib-provided/");
	}

	@Override
	protected String getEntryPathPrefix() {
		return "WEB-INF/";
	}

	public static void main(String[] args) throws Exception {
		new WarLauncher().launch(args);
	}

}
