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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.springframework.boot.loader.util.SystemPropertyUtils;
import org.springframework.util.ResourceUtils;

/**
 * <p>
 * {@link Launcher} for archives with user-configured classpath and main class via a
 * properties file. This model is often more flexible and more amenable to creating
 * well-behaved OS-level services than a model based on executable jars.
 * </p>
 * 
 * <p>
 * Looks in various places for a properties file to extract loader settings, defaulting to
 * <code>application.properties</code> either on the current classpath or in the current
 * working directory. The name of the properties file can be changed by setting a System
 * property <code>loader.config.name</code> (e.g. <code>-Dloader.config.name=foo</code>
 * will look for <code>foo.properties</code>. If that file doesn't exist then tries
 * <code>loader.config.location</code> (with allowed prefixes <code>classpath:</code> and
 * <code>file:</code> or any valid URL). Once that file is located turns it into
 * Properties and extracts optional values (which can also be provided oroverridden as
 * System properties in case the file doesn't exist):
 * 
 * <ul>
 * <li><code>loader.path</code>: a comma-separated list of classpath directories
 * (containing file resources and/or archives in *.jar or *.zip). Defaults to
 * <code>lib</code> (i.e. a directory in the current working directory)</li>
 * <li><code>loader.main</code>: the main method to delegate execution to once the class
 * loader is set up. No default, but will fall back to looking in a
 * <code>MANIFEST.MF</code> if there is one.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * 
 * </p>
 * 
 * @author Dave Syer
 */
public class PropertiesLauncher extends Launcher {

	/**
	 * Properties key for main class
	 */
	public static final String MAIN = "loader.main";

	/**
	 * Properties key for classpath entries (directories possibly containing jars)
	 */
	public static final String PATH = "loader.path";

	public static final String HOME = "loader.home";

	public static String CONFIG_NAME = "loader.config.name";

	public static String CONFIG_LOCATION = "loader.config.location";

	private Logger logger = Logger.getLogger(Launcher.class.getName());

	private static final List<String> DEFAULT_PATHS = Arrays.asList("lib/");

	private List<String> paths = new ArrayList<String>(DEFAULT_PATHS);

	private Properties properties = new Properties();

	@Override
	public void launch(String[] args) {
		try {
			launch(args, new ExplodedArchive(new File(getHomeDirectory())));
		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	protected String getHomeDirectory() {
		return SystemPropertyUtils.resolvePlaceholders(System.getProperty(HOME,
				"${user.dir}"));
	}

	@Override
	protected boolean isNestedArchive(Archive.Entry entry) {
		String name = entry.getName();
		if (entry.isDirectory()) {
			for (String path : this.paths) {
				if (path.length() > 0 && name.equals(path)) {
					return true;
				}
			}
		}
		else {
			for (String path : this.paths) {
				if (path.length() > 0 && name.startsWith(path) && isArchive(name)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void postProcessLib(Archive archive, List<Archive> lib) throws Exception {
		lib.add(0, archive);
	}

	/**
	 * Look in various places for a properties file to extract loader settings. Default to
	 * <code>application.properties</code> either on the current classpath or in the
	 * current working directory.
	 * 
	 * @see org.springframework.boot.loader.Launcher#launch(java.lang.String[],
	 * org.springframework.boot.loader.Archive)
	 */
	@Override
	protected void launch(String[] args, Archive archive) throws Exception {
		initialize();
		super.launch(args, archive);
	}

	protected void initialize() throws Exception {

		String config = SystemPropertyUtils.resolvePlaceholders(System.getProperty(
				CONFIG_NAME, "application")) + ".properties";
		while (config.startsWith("/")) {
			config = config.substring(1);
		}
		this.logger.fine("Trying default location: " + config);
		InputStream resource = getClass().getResourceAsStream("/" + config);
		if (resource == null) {

			config = SystemPropertyUtils.resolvePlaceholders(System.getProperty(
					CONFIG_LOCATION, config));

			if (config.startsWith("classpath:")) {

				config = config.substring("classpath:".length());
				while (config.startsWith("/")) {
					config = config.substring(1);
				}
				config = "/" + config;
				this.logger.fine("Trying classpath: " + config);
				resource = getClass().getResourceAsStream(config);

			}
			else {

				if (config.startsWith("file:")) {

					config = config.substring("file:".length());
					if (config.startsWith("//")) {
						config = config.substring(2);
					}

				}
				if (!config.contains(":")) {

					File file = new File(config);
					this.logger.fine("Trying file: " + config);
					if (file.canRead()) {
						resource = new FileInputStream(file);
					}

				}
				else {

					URL url = new URL(config);
					if (exists(url)) {
						URLConnection con = url.openConnection();
						try {
							resource = con.getInputStream();
						}
						catch (IOException ex) {
							// Close the HTTP connection (if applicable).
							if (con instanceof HttpURLConnection) {
								((HttpURLConnection) con).disconnect();
							}
							throw ex;
						}
					}
				}
			}
		}
		if (resource != null) {
			this.logger.info("Found: " + config);
			this.properties.load(resource);
			try {
				String path = System.getProperty(PATH);
				if (path == null) {
					path = this.properties.getProperty(PATH);
				}
				if (path != null) {
					path = SystemPropertyUtils.resolvePlaceholders(path);
					this.paths = new ArrayList<String>(Arrays.asList(path.split(",")));
					for (int i = 0; i < this.paths.size(); i++) {
						this.paths.set(i, this.paths.get(i).trim());
					}
				}
			}
			finally {
				resource.close();
			}
		}
		else {
			this.logger.info("Not found: " + config);
		}
		for (int i = 0; i < this.paths.size(); i++) {
			if (!this.paths.get(i).endsWith("/")) {
				// Always a directory
				this.paths.set(i, this.paths.get(i) + "/");
			}
			if (this.paths.get(i).startsWith("./")) {
				// No need for current dir path
				this.paths.set(i, this.paths.get(i).substring(2));
			}
		}
		for (Iterator<String> iter = this.paths.iterator(); iter.hasNext();) {
			String path = iter.next();
			if (path.equals(".") || path.equals("")) {
				// Empty path is always on the classpath so no need for it to be
				// explicitly listed here
				iter.remove();
			}
		}
		this.logger.info("Nested archive paths: " + this.paths);
	}

	private boolean exists(URL url) throws IOException {

		// Try a URL connection content-length header...
		URLConnection con = url.openConnection();
		ResourceUtils.useCachesIfNecessary(con);
		HttpURLConnection httpCon = (con instanceof HttpURLConnection ? (HttpURLConnection) con
				: null);
		if (httpCon != null) {
			httpCon.setRequestMethod("HEAD");
			int code = httpCon.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				return true;
			}
			else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
				return false;
			}
		}
		if (con.getContentLength() >= 0) {
			return true;
		}
		if (httpCon != null) {
			// no HTTP OK status, and no content-length header: give up
			httpCon.disconnect();
		}
		return false;

	}

	@Override
	protected String getMainClass(Archive archive) throws Exception {
		if (System.getProperty(MAIN) != null) {
			return SystemPropertyUtils.resolvePlaceholders(System.getProperty(MAIN));
		}
		if (this.properties.containsKey(MAIN)) {
			return SystemPropertyUtils.resolvePlaceholders(this.properties
					.getProperty(MAIN));
		}
		return super.getMainClass(archive);
	}

	public static void main(String[] args) {
		new PropertiesLauncher().launch(args);
	}

}
