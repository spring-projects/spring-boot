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

package org.springframework.bootstrap;

import java.io.PrintStream;

/**
 * Writes the 'Spring' banner.
 * 
 * @author Phillip Webb
 */
abstract class Banner {

	private static final String[] BANNER = { "  .   ____          _            __ _ _",
			" /\\\\ / ___'_ __ _ _(_)_ __  __ _ \\ \\ \\ \\",
			"( ( )\\___ | '_ | '_| | '_ \\/ _` | \\ \\ \\ \\",
			" \\\\/  ___)| |_)| | | | | || (_| |  ) ) ) )",
			"  '  |____| .__|_| |_|_| |_\\__, | / / / /",
			" =========|_|==============|___/=/_/_/_/" };

	/**
	 * Write the banner to the specified print stream.
	 * @param printStream the output print stream
	 */
	public static void write(PrintStream printStream) {
		printStream.println();
		for (String line : BANNER) {
			printStream.println(line);
		}
		String version = Banner.class.getPackage().getImplementationVersion();
		printStream.println("  Spring Bootstrap"
				+ (version == null ? "" : " (v" + version + ")"));
		printStream.println();
	}

}
