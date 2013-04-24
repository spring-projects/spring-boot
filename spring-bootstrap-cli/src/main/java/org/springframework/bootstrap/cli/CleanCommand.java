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

import org.apache.ivy.util.FileUtil;

/**
 * {@link Command} to 'clean' up grapes.
 * 
 * @author Dave Syer
 * 
 */
public class CleanCommand extends AbstractCommand {

	public CleanCommand() {
		super("clean",
				"Clean up groovy grapes (useful if snapshots are needed and you need an update)");
	}

	@Override
	public void run(String... args) throws Exception {

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
		// TODO: add support for other packages as args
		String[] packages = new String[] { "org.springframework.bootstrap" };
		for (String pkg : packages) {
			File file = new File(grapes, pkg);
			if (file.exists()) {
				FileUtil.forceDelete(file);
			}
		}

	}

}
