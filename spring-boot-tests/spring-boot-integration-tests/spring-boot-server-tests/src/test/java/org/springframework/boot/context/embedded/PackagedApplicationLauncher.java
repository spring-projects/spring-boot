/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.testsupport.BuildOutput;

/**
 * {@link AbstractApplicationLauncher} that launches a packaged Spring Boot application
 * using {@code java -jar}.
 *
 * @author Andy Wilkinson
 */
class PackagedApplicationLauncher extends AbstractApplicationLauncher {

	PackagedApplicationLauncher(ApplicationBuilder applicationBuilder,
			BuildOutput buildOutput) {
		super(applicationBuilder, buildOutput);
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
		return Arrays.asList("-jar", archive.getAbsolutePath(),
				serverPortFile.getAbsolutePath());
	}

}
