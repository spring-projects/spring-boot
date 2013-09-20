/*
 * Copyright 2013 the original author or authors.
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

import java.io.File;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for launchers that can start an application with a fully configured
 * classpath.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AbstractLauncher implements ArchiveFilter {

	private Logger logger = Logger.getLogger(AbstractLauncher.class.getName());

	private LaunchHelper helper = new LaunchHelper();

	/**
	 * Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 * @param args the incoming arguments
	 */
	public void launch(String[] args) {
		try {
			launch(args, getClass().getProtectionDomain());
		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Launch the application given the protection domain.
	 * @param args the incoming arguments
	 * @param protectionDomain the protection domain
	 * @throws Exception
	 */
	protected void launch(String[] args, ProtectionDomain protectionDomain)
			throws Exception {
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
		String path = (location == null ? null : location.getPath());
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}
		File root = new File(path);
		if (!root.exists()) {
			throw new IllegalStateException(
					"Unable to determine code source archive from " + root);
		}
		Archive archive = (root.isDirectory() ? new ExplodedArchive(root)
				: new JarFileArchive(root));
		launch(args, archive);
	}

	/**
	 * Launch the application given the archive file
	 * @param args the incoming arguments
	 * @param archive the underlying (zip/war/jar) archive
	 * @throws Exception
	 */
	protected void launch(String[] args, Archive archive) throws Exception {
		List<Archive> lib = new ArrayList<Archive>();
		lib.addAll(this.helper.findNestedArchives(archive, this));
		this.logger.fine("Added " + lib.size() + " entries");
		postProcessLib(archive, lib);
		String mainClass = this.helper.getMainClass(archive);
		this.helper.launch(args, mainClass, lib);
	}

	/**
	 * Called to post-process lib entries before they are used. Implementations can add
	 * and remove entries.
	 * @param archive the archive
	 * @param lib the existing lib
	 * @throws Exception
	 */
	protected void postProcessLib(Archive archive, List<Archive> lib) throws Exception {
	}

}
