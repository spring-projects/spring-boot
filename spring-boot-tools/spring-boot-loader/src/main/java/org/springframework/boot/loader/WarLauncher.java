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

package org.springframework.boot.loader;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 */
public class WarLauncher extends ExecutableArchiveLauncher {

	private static final AsciiBytes WEB_INF = new AsciiBytes("WEB-INF/");

	private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");

	private static final AsciiBytes WEB_INF_CLASSES = WEB_INF.append("classes/");

	private static final AsciiBytes WEB_INF_LIB = WEB_INF.append("lib/");

	private static final AsciiBytes WEB_INF_LIB_PROVIDED = WEB_INF
			.append("lib-provided/");

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

	@Override
	protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
		archives.add(0, getFilteredArchive());
	}

	/**
	 * Filter the specified WAR file to exclude elements that should not appear on the
	 * classpath.
	 * @return the filtered archive
	 * @throws IOException on error
	 */
	protected Archive getFilteredArchive() throws IOException {
		return getArchive().getFilteredArchive(new Archive.EntryRenameFilter() {
			@Override
			public AsciiBytes apply(AsciiBytes entryName, Archive.Entry entry) {
				if (entryName.startsWith(META_INF) || entryName.startsWith(WEB_INF)) {
					return null;
				}
				return entryName;
			}
		});
	}

	public static void main(String[] args) {
		new WarLauncher().launch(args);
	}

}
