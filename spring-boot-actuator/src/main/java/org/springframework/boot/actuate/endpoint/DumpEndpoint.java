/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link Endpoint} to expose thread info.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "endpoints.dump", ignoreUnknownFields = false)
public class DumpEndpoint extends AbstractEndpoint<List<ThreadInfo>> {

	/**
	 * Create a new {@link DumpEndpoint} instance.
	 */
	public DumpEndpoint() {
		super("dump");
	}

	@Override
	public List<ThreadInfo> invoke() {
		return Arrays.asList(ManagementFactory.getThreadMXBean().dumpAllThreads(true,
				true));
	}

}
