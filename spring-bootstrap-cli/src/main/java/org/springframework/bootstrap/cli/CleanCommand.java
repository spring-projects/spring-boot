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

	private static enum Layout {
		IVY, MAVEN;
	}

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
		clean(options, getGrapesHome(options), Layout.IVY);
		clean(options, getMavenHome(options), Layout.MAVEN);
	}

	private void clean(OptionSet options, File root, Layout layout) {

		if (root == null || !root.exists()) {
			return;
		}

		ArrayList<String> specs = new ArrayList<String>(options.nonOptionArguments());
		if (!specs.contains("org.springframework.bootstrap") && layout == Layout.IVY) {
			specs.add(0, "org.springframework.bootstrap");
		}
		for (String spec : specs) {
			String group = spec;
			String module = null;
			if (spec.contains(":")) {
				group = spec.substring(0, spec.indexOf(":"));
				module = spec.substring(spec.indexOf(":") + 1);
			}
			File file = getModulePath(root, group, module, layout);
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

	private File getModulePath(File root, String group, String module, Layout layout) {
		File parent = root;
		if (layout == Layout.IVY) {
			parent = new File(parent, group);
		} else {
			for (String path : group.split("\\.")) {
				parent = new File(parent, path);
			}
		}

		if (module == null) {
			return parent;
		}
		return new File(parent, module);
	}

	private File getGrapesHome(OptionSet options) {

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
			return null;
		}

		File grapes = new File(home, "grapes");
		return grapes;
	}

	private File getMavenHome(OptionSet options) {
		String dir = System.getProperty("user.home");

		if (dir == null || !new File(dir).exists()) {
			return null;
		}
		File home = new File(dir);
		File grapes = new File(new File(home, ".m2"), "repository");
		return grapes;
	}
}
