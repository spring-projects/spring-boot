/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;

/**
 * Launches an application with DevTools.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public interface ApplicationLauncher {

	LaunchedApplication launchApplication(JvmLauncher javaLauncher, File serverPortFile) throws Exception;

	LaunchedApplication launchApplication(JvmLauncher jvmLauncher, File serverPortFile, String... additionalArgs)
			throws Exception;

}
