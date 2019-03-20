/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader;

import org.springframework.boot.loader.archive.Archive;

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class WarLauncher extends ExecutableArchiveLauncher {

	private static final String WEB_INF = "WEB-INF/";

	private static final String WEB_INF_CLASSES = WEB_INF + "classes/";

	private static final String WEB_INF_LIB = WEB_INF + "lib/";

	private static final String WEB_INF_LIB_PROVIDED = WEB_INF + "lib-provided/";

	public WarLauncher() {
		super();
	}

	protected WarLauncher(Archive archive) {
		super(archive);
	}

	@Override
	public boolean isNestedArchive(Archive.Entry entry) {
		if (entry.isDirectory()) {
			return entry.getName().equals(WEB_INF_CLASSES);
		}
		else {
			return entry.getName().startsWith(WEB_INF_LIB)
					|| entry.getName().startsWith(WEB_INF_LIB_PROVIDED);
		}
	}

	public static void main(String[] args) throws Exception {
		new WarLauncher().launch(args);
	}

}
