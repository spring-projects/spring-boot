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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.List;

import com.sun.tools.attach.VirtualMachine;

/**
 * @author Dave Syer
 */
public abstract class AgentAttacher {

	public static void attach(File agent) {
		String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
		int p = nameOfRunningVM.indexOf('@');
		String pid = nameOfRunningVM.substring(0, p);

		try {
			VirtualMachine vm = VirtualMachine.attach(pid);
			vm.loadAgent(agent.getAbsolutePath());
			vm.detach();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static List<String> commandLineArguments() {
		return ManagementFactory.getRuntimeMXBean().getInputArguments();
	}

	public static boolean hasNoVerify() {
		return commandLineArguments().contains("-Xverify:none");
	}

}
