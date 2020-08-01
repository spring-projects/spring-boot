/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.boot.ApplicationArguments;
import org.springframework.util.ClassUtils;

/**
 * @author Madhura Bhave
 */
public interface Command {

	default String getName() {
		String name = ClassUtils.getShortName(getClass());
		int lastDot = name.lastIndexOf(".");
		if (lastDot != -1) {
			name = name.substring(lastDot + 1, name.length());
		}
		if (name.endsWith("Command")) {
			name = name.substring(0, name.length() - "Command".length());
		}
		return name.toLowerCase();
	}

	void run(ApplicationArguments args) throws Exception;

}