/*
 * Copyright 2012-2023 the original author or authors.
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/**
 * Formats a thread dump as plain text.
 *
 * @author Andy Wilkinson
 */
class PlainTextThreadDumpFormatter {

	/**
     * Formats an array of ThreadInfo objects into a string representation.
     * 
     * @param threads the array of ThreadInfo objects to be formatted
     * @return the formatted string representation of the ThreadInfo objects
     */
    String format(ThreadInfo[] threads) {
		StringWriter dump = new StringWriter();
		PrintWriter writer = new PrintWriter(dump);
		writePreamble(writer);
		for (ThreadInfo info : threads) {
			writeThread(writer, info);
		}
		return dump.toString();
	}

	/**
     * Writes the preamble for the thread dump report.
     * 
     * The preamble includes the current date and time, the VM name, version, and information.
     * 
     * @param writer the PrintWriter object used to write the preamble
     */
    private void writePreamble(PrintWriter writer) {
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		writer.println(dateFormat.format(LocalDateTime.now()));
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		writer.printf("Full thread dump %s (%s %s):%n", runtime.getVmName(), runtime.getVmVersion(),
				System.getProperty("java.vm.info"));
		writer.println();
	}

	/**
     * Writes the information about a thread to the given PrintWriter.
     *
     * @param writer the PrintWriter to write the thread information to
     * @param info   the ThreadInfo object containing the thread information
     */
    private void writeThread(PrintWriter writer, ThreadInfo info) {
		writer.printf("\"%s\" - Thread t@%d%n", info.getThreadName(), info.getThreadId());
		writer.printf("   %s: %s%n", Thread.State.class.getCanonicalName(), info.getThreadState());
		writeStackTrace(writer, info, info.getLockedMonitors());
		writer.println();
		writeLockedOwnableSynchronizers(writer, info);
		writer.println();
	}

	/**
     * Writes the stack trace of a thread to the specified PrintWriter.
     * 
     * @param writer The PrintWriter to write the stack trace to.
     * @param info The ThreadInfo object containing the stack trace information.
     * @param lockedMonitors An array of MonitorInfo objects representing the locked monitors.
     */
    private void writeStackTrace(PrintWriter writer, ThreadInfo info, MonitorInfo[] lockedMonitors) {
		int depth = 0;
		for (StackTraceElement element : info.getStackTrace()) {
			writeStackTraceElement(writer, element, info, lockedMonitorsForDepth(lockedMonitors, depth), depth == 0);
			depth++;
		}
	}

	/**
     * Returns a list of MonitorInfo objects that are locked at the specified depth.
     * 
     * @param lockedMonitors an array of MonitorInfo objects representing locked monitors
     * @param depth the depth at which the monitors are locked
     * @return a list of MonitorInfo objects locked at the specified depth
     */
    private List<MonitorInfo> lockedMonitorsForDepth(MonitorInfo[] lockedMonitors, int depth) {
		return Stream.of(lockedMonitors).filter((candidate) -> candidate.getLockedStackDepth() == depth).toList();
	}

	/**
     * Writes the stack trace element to the provided PrintWriter.
     * 
     * @param writer the PrintWriter to write the stack trace element to
     * @param element the StackTraceElement to be written
     * @param info the ThreadInfo containing additional information about the thread
     * @param lockedMonitors the list of locked monitors
     * @param firstElement a boolean indicating if this is the first stack trace element
     */
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

	/**
     * Formats the given LockInfo object into a string representation.
     * 
     * @param lockInfo the LockInfo object to be formatted
     * @return the formatted string representation of the LockInfo object
     */
    private String format(LockInfo lockInfo) {
		return String.format("<%x> (a %s)", lockInfo.getIdentityHashCode(), lockInfo.getClassName());
	}

	/**
     * Writes the information about locked monitors to the provided PrintWriter.
     * 
     * @param writer The PrintWriter to write the information to.
     * @param lockedMonitorsAtCurrentDepth The list of MonitorInfo objects representing the locked monitors at the current depth.
     */
    private void writeMonitors(PrintWriter writer, List<MonitorInfo> lockedMonitorsAtCurrentDepth) {
		for (MonitorInfo lockedMonitor : lockedMonitorsAtCurrentDepth) {
			writer.printf("\t- locked %s%n", format(lockedMonitor));
		}
	}

	/**
     * Writes the locked ownable synchronizers information to the specified PrintWriter.
     * 
     * @param writer the PrintWriter to write the information to
     * @param info the ThreadInfo object containing the thread information
     */
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
