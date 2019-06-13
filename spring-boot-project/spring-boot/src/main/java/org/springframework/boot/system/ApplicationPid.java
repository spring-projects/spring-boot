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

package org.springframework.boot.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * An application process ID.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationPid {

	private static final PosixFilePermission[] WRITE_PERMISSIONS = { PosixFilePermission.OWNER_WRITE,
			PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE };

	private final String pid;

	public ApplicationPid() {
		this.pid = getPid();
	}

	protected ApplicationPid(String pid) {
		this.pid = pid;
	}

	private String getPid() {
		try {
			String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			return jvmName.split("@")[0];
		}
		catch (Throwable ex) {
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ApplicationPid) {
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
		createParentFolder(file);
		if (file.exists()) {
			assertCanOverwrite(file);
		}
		try (FileWriter writer = new FileWriter(file)) {
			writer.append(this.pid);
		}
	}

	private void createParentFolder(File file) {
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
