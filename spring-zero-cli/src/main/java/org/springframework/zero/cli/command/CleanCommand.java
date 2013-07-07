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

package org.springframework.zero.cli.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.ivy.util.FileUtil;
import org.springframework.zero.cli.Command;
import org.springframework.zero.cli.Log;

/**
 * {@link Command} to 'clean' up grapes, removing cached dependencies and forcing a
 * download on the next attempt to resolve.
 * 
 * @author Dave Syer
 */
public class CleanCommand extends OptionParsingCommand {

	public CleanCommand() {
		super("clean", "Clean up groovy grapes "
				+ "(useful if snapshots are needed and you need an update)",
				new CleanOptionHandler());
	}

	@Override
	public String getUsageHelp() {
		return "[options] <dependencies>";
	}

	private static class CleanOptionHandler extends OptionHandler {

		private OptionSpec<Void> allOption;

		private OptionSpec<Void> ivyOption;

		private OptionSpec<Void> mvnOption;

		@Override
		protected void options() {
			this.allOption = option("all", "Clean all files (not just snapshots)");
			this.ivyOption = option("ivy", "Clean just ivy (grapes) cache. "
					+ "Default is on unless --maven is used.");
			this.mvnOption = option("maven", "Clean just maven cache. Default is off.");
		}

		@Override
		protected void run(OptionSet options) throws Exception {
			if (!options.has(this.ivyOption)) {
				clean(options, getGrapesHome(), Layout.IVY);
			}
			if (options.has(this.mvnOption)) {
				if (options.has(this.ivyOption)) {
					clean(options, getGrapesHome(), Layout.IVY);
				}
				clean(options, getMavenHome(), Layout.MAVEN);
			}
		}

		private void clean(OptionSet options, File root, Layout layout) {
			if (root == null || !root.exists()) {
				return;
			}
			ArrayList<String> specs = new ArrayList<String>(options.nonOptionArguments());
			if (!specs.contains("org.springframework.zero") && layout == Layout.IVY) {
				specs.add(0, "org.springframework.zero");
			}
			for (String spec : specs) {
				clean(options, root, layout, spec);
			}
		}

		private void clean(OptionSet options, File root, Layout layout, String spec) {
			String group = spec;
			String module = null;
			if (spec.contains(":")) {
				group = spec.substring(0, spec.indexOf(':'));
				module = spec.substring(spec.indexOf(':') + 1);
			}

			File file = getModulePath(root, group, module, layout);
			if (!file.exists()) {
				return;
			}

			if (options.has(this.allOption) || group.equals("org.springframework.zero")) {
				delete(file);
				return;
			}

			for (Object obj : FileUtil.listAll(file, Collections.emptyList())) {
				File candidate = (File) obj;
				if (candidate.getName().contains("SNAPSHOT")) {
					delete(candidate);
				}
			}
		}

		private void delete(File file) {
			Log.info("Deleting: " + file);
			FileUtil.forceDelete(file);
		}

		private File getModulePath(File root, String group, String module, Layout layout) {
			File parent = root;
			if (layout == Layout.IVY) {
				parent = new File(parent, group);
			}
			else {
				for (String path : group.split("\\.")) {
					parent = new File(parent, path);
				}
			}

			if (module == null) {
				return parent;
			}
			return new File(parent, module);
		}

		private File getGrapesHome() {
			String dir = System.getenv("GROOVY_HOME");
			String userdir = System.getProperty("user.home");
			File home;
			if (dir == null || !new File(dir).exists()) {
				dir = userdir;
				home = new File(dir, ".groovy");
			}
			else {
				home = new File(dir);
			}
			if (dir == null || !new File(dir).exists()) {
				return null;
			}
			return new File(home, "grapes");
		}

		private File getMavenHome() {
			String dir = System.getProperty("user.home");
			if (dir == null || !new File(dir).exists()) {
				return null;
			}
			File home = new File(dir);
			return new File(new File(home, ".m2"), "repository");
		}

		private static enum Layout {
			IVY, MAVEN;
		}

	}

}
