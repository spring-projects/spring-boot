/*
 * Copyright 2012-2020 the original author or authors.
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

package io.spring.concourse.releasescripts.command;

import java.util.List;

import io.spring.concourse.releasescripts.ReleaseType;
import io.spring.concourse.releasescripts.sdkman.SdkmanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Command used to publish to SDKMAN.
 *
 * @author Madhura Bhave
 */
@Component
public class PublishToSdkmanCommand implements Command {

	private static final Logger logger = LoggerFactory.getLogger(PublishToSdkmanCommand.class);

	private final SdkmanService service;

	public PublishToSdkmanCommand(SdkmanService service) {
		this.service = service;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		logger.debug("Running 'push to SDKMAN' command");
		List<String> nonOptionArgs = args.getNonOptionArgs();
		Assert.state(!nonOptionArgs.isEmpty(), "No command argument specified");
		Assert.state(nonOptionArgs.size() >= 3, "Release type or version not specified");
		String releaseType = nonOptionArgs.get(1);
		ReleaseType type = ReleaseType.from(releaseType);
		if (!ReleaseType.RELEASE.equals(type)) {
			return;
		}
		String version = nonOptionArgs.get(2);
		boolean makeDefault = false;
		if (nonOptionArgs.size() == 4) {
			String releaseBranch = nonOptionArgs.get(3);
			makeDefault = ("master".equals(releaseBranch));
		}
		this.service.publish(version, makeDefault);
	}

}
