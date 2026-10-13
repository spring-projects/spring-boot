/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.embedded;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * {@link AbstractApplicationLauncher} that launches a packaged Spring Boot application
 * using {@code java -jar}.
 *
 * @author Andy Wilkinson
 */
class PackagedApplicationLauncher extends AbstractApplicationLauncher {

	PackagedApplicationLauncher(Application application, File outputLocation) {
		super(application, outputLocation);
	}

	@Override
	protected File getWorkingDirectory() {
		return null;
	}

	@Override
	protected String getDescription(String packaging) {
		return "packaged " + packaging;
	}

	@Override
	protected List<String> getArguments(File archive, File serverPortFile) {
		return Arrays.asList("-jar", archive.getAbsolutePath(), serverPortFile.getAbsolutePath());
	}

}
