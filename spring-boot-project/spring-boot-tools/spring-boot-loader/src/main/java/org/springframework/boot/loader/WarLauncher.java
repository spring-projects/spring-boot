/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.boot.loader.archive.Archive.Entry;

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class WarLauncher extends ExecutableArchiveLauncher {

	public WarLauncher() {
	}

	protected WarLauncher(Archive archive) {
		super(archive);
	}

	@Override
	protected boolean isPostProcessingClassPathArchives() {
		return false;
	}

	@Override
	protected boolean isSearchCandidate(Entry entry) {
		return entry.getName().startsWith("WEB-INF/");
	}

	@Override
	public boolean isNestedArchive(Archive.Entry entry) {
		if (entry.isDirectory()) {
			return entry.getName().equals("WEB-INF/classes/");
		}
		return entry.getName().startsWith("WEB-INF/lib/") || entry.getName().startsWith("WEB-INF/lib-provided/");
	}

	public static void main(String[] args) throws Exception {
		new WarLauncher().launch(args);
	}

}
