/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * An application process ID.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationPid {

	private static final Log logger = LogFactory.getLog(ApplicationPid.class);

	private static final PosixFilePermission[] WRITE_PERMISSIONS = { PosixFilePermission.OWNER_WRITE,
			PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE };

	private static final long JVM_NAME_RESOLVE_THRESHOLD = 200;

	private final String pid;

	public ApplicationPid() {
		this.pid = getPid();
	}

	protected ApplicationPid(String pid) {
		this.pid = pid;
	}

	private String getPid() {
		try {
			String jvmName = resolveJvmName();
			return jvmName.split("@")[0];
		}
		catch (Throwable ex) {
			return null;
		}
	}

	private String resolveJvmName() {
		long startTime = System.currentTimeMillis();
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		long elapsed = System.currentTimeMillis() - startTime;
		if (elapsed > JVM_NAME_RESOLVE_THRESHOLD) {
			logger.warn(LogMessage.of(() -> {
				StringBuilder warning = new StringBuilder();
				warning.append("ManagementFactory.getRuntimeMXBean().getName() took ");
				warning.append(elapsed);
				warning.append(" milliseconds to respond.");
				warning.append(" This may be due to slow host name resolution.");
				warning.append(" Please verify your network configuration");
				if (System.getProperty("os.name").toLowerCase().contains("mac")) {
					warning.append(" (macOS machines may need to add entries to /etc/hosts)");
				}
				warning.append(".");
				return warning;
			}));
		}
		return jvmName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ApplicationPid) {
			return ObjectUtils.nullSafeEquals(this.pid, ((ApplicationPid) obj).pid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.pid);
	}

	@Override
	public String toString() {
		return (this.pid != null) ? this.pid : "???";
	}

	/**
	 * Write the PID to the specified file.
	 * @param file the PID file
	 * @throws IllegalStateException if no PID is available.
	 * @throws IOException if the file cannot be written
	 */
	public void write(File file) throws IOException {
		Assert.state(this.pid != null, "No PID available");
		createParentDirectory(file);
		if (file.exists()) {
			assertCanOverwrite(file);
		}
		try (FileWriter writer = new FileWriter(file)) {
			writer.append(this.pid);
		}
	}

	private void createParentDirectory(File file) {
		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
	}

	private void assertCanOverwrite(File file) throws IOException {
		if (!file.canWrite() || !canWritePosixFile(file)) {
			throw new FileNotFoundException(file.toString() + " (permission denied)");
		}
	}

	private boolean canWritePosixFile(File file) throws IOException {
		try {
			Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file.toPath());
			for (PosixFilePermission permission : WRITE_PERMISSIONS) {
				if (permissions.contains(permission)) {
					return true;
				}
			}
			return false;
		}
		catch (UnsupportedOperationException ex) {
			// Assume that we can
			return true;
		}
	}

}
