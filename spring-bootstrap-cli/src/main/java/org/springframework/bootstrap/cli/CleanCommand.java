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
package org.springframework.bootstrap.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.ivy.util.FileUtil;

/**
 * {@link Command} to 'clean' up grapes, removing cached dependencies and forcing a
 * download on the next attempt to resolve.
 * 
 * @author Dave Syer
 * 
 */
public class CleanCommand extends OptionParsingCommand {

	private OptionSpec<Void> allOption;

	public CleanCommand() {
		super("clean",
				"Clean up groovy grapes (useful if snapshots are needed and you need an update)");
	}

	@Override
	public String getUsageHelp() {
		return "[options] <dependencies>";
	}

	@Override
	protected OptionParser createOptionParser() {
		OptionParser parser = new OptionParser();
		this.allOption = parser.accepts("all", "Clean all files (not just snapshots)");
		return parser;
	}

	@Override
	protected void run(OptionSet options) throws Exception {

		String dir = System.getenv("GROOVY_HOME");
		String userdir = System.getProperty("user.home");

		File home;
		if (dir == null || !new File(dir).exists()) {
			dir = userdir;
			home = new File(dir, ".groovy");
		} else {
			home = new File(dir);
		}
		if (dir == null || !new File(dir).exists()) {
			return;
		}

		if (!home.exists()) {
			return;
		}

		File grapes = new File(home, "grapes");
		ArrayList<String> specs = new ArrayList<String>(options.nonOptionArguments());
		if (!specs.contains("org.springframework.bootstrap")) {
			specs.add(0, "org.springframework.bootstrap");
		}
		for (String spec : specs) {
			String group = spec;
			String module = null;
			if (spec.contains(":")) {
				group = spec.substring(0, spec.indexOf(":"));
				module = spec.substring(spec.indexOf(":") + 1);
			}
			File file = module == null ? new File(grapes, group) : new File(new File(
					grapes, group), module);
			if (file.exists()) {
				if (options.has(this.allOption)
						|| group.equals("org.springframework.bootstrap")) {
					FileUtil.forceDelete(file);
				} else {
					for (Object obj : FileUtil.listAll(file, Collections.emptyList())) {
						File candidate = (File) obj;
						if (candidate.getName().contains("SNAPSHOT")) {
							FileUtil.forceDelete(candidate);
						}
					}
				}
			}
		}

	}

}
