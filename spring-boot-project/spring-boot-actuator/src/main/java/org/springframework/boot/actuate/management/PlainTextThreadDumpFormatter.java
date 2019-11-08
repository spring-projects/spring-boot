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

package org.springframework.boot.actuate.management;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Formats a thread dump as plain text.
 *
 * @author Andy Wilkinson
 */
class PlainTextThreadDumpFormatter {

	String format(ThreadInfo[] threads) {
		StringWriter dump = new StringWriter();
		PrintWriter writer = new PrintWriter(dump);
		writePreamble(writer);
		for (ThreadInfo info : threads) {
			writeThread(writer, info);
		}
		return dump.toString();
	}

	private void writePreamble(PrintWriter writer) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		writer.println(dateFormat.format(new Date()));
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		writer.printf("Full thread dump %s (%s %s):%n", runtime.getVmName(), runtime.getVmVersion(),
				System.getProperty("java.vm.info"));
		writer.println();
	}

	private void writeThread(PrintWriter writer, ThreadInfo info) {
		writer.printf("\"%s\" - Thread t@%d%n", info.getThreadName(), info.getThreadId());
		writer.printf("   %s: %s%n", Thread.State.class.getCanonicalName(), info.getThreadState());
		writeStackTrace(writer, info, info.getLockedMonitors());
		writer.println();
		writeLockedOwnableSynchronizers(writer, info);
		writer.println();
	}

	private void writeStackTrace(PrintWriter writer, ThreadInfo info, MonitorInfo[] lockedMonitors) {
		int depth = 0;
		for (StackTraceElement element : info.getStackTrace()) {
			writeStackTraceElement(writer, element, info, lockedMonitorsForDepth(lockedMonitors, depth), depth == 0);
			depth++;
		}
	}

	private List<MonitorInfo> lockedMonitorsForDepth(MonitorInfo[] lockedMonitors, int depth) {
		return Stream.of(lockedMonitors).filter((lockedMonitor) -> lockedMonitor.getLockedStackDepth() == depth)
				.collect(Collectors.toList());
	}

	private void writeStackTraceElement(PrintWriter writer, StackTraceElement element, ThreadInfo info,
			List<MonitorInfo> lockedMonitors, boolean firstElement) {
		writer.printf("\tat %s%n", element.toString());
		LockInfo lockInfo = info.getLockInfo();
		if (firstElement && lockInfo != null) {
			if (element.getClassName().equals(Object.class.getName()) && element.getMethodName().equals("wait")) {
				writer.printf("\t- waiting on %s%n", format(lockInfo));
			}
			else {
				String lockOwner = info.getLockOwnerName();
				if (lockOwner != null) {
					writer.printf("\t- waiting to lock %s owned by \"%s\" t@%d%n", format(lockInfo), lockOwner,
							info.getLockOwnerId());
				}
				else {
					writer.printf("\t- parking to wait for %s%n", format(lockInfo));
				}
			}
		}
		writeMonitors(writer, lockedMonitors);
	}

	private String format(LockInfo lockInfo) {
		return String.format("<%x> (a %s)", lockInfo.getIdentityHashCode(), lockInfo.getClassName());
	}

	private void writeMonitors(PrintWriter writer, List<MonitorInfo> lockedMonitorsAtCurrentDepth) {
		for (MonitorInfo lockedMonitor : lockedMonitorsAtCurrentDepth) {
			writer.printf("\t- locked %s%n", format(lockedMonitor));
		}
	}

	private void writeLockedOwnableSynchronizers(PrintWriter writer, ThreadInfo info) {
		writer.println("   Locked ownable synchronizers:");
		LockInfo[] lockedSynchronizers = info.getLockedSynchronizers();
		if (lockedSynchronizers == null || lockedSynchronizers.length == 0) {
			writer.println("\t- None");
		}
		else {
			for (LockInfo lockedSynchronizer : lockedSynchronizers) {
				writer.printf("\t- Locked %s%n", format(lockedSynchronizer));
			}
		}
	}

}
