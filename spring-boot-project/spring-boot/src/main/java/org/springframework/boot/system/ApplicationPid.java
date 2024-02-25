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

package org.springframework.boot.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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

	/**
	 * Constructs a new ApplicationPid object.
	 *
	 * This constructor initializes the pid field by calling the getPid() method.
	 */
	public ApplicationPid() {
		this.pid = getPid();
	}

	/**
	 * Constructs a new ApplicationPid object with the specified PID.
	 * @param pid the process ID (PID) of the application
	 */
	protected ApplicationPid(String pid) {
		this.pid = pid;
	}

	/**
	 * Returns the process ID (PID) of the current process.
	 * @return the process ID (PID) as a string, or null if an error occurs
	 */
	private String getPid() {
		try {
			return Long.toString(ProcessHandle.current().pid());
		}
		catch (Throwable ex) {
			return null;
		}
	}

	/**
	 * Compares this ApplicationPid object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the specified object is equal to this ApplicationPid object, false
	 * otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ApplicationPid other) {
			return ObjectUtils.nullSafeEquals(this.pid, other.pid);
		}
		return false;
	}

	/**
	 * Returns a hash code value for the object. This method overrides the hashCode()
	 * method in the Object class.
	 * @return the hash code value for the object
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.pid);
	}

	/**
	 * Returns a string representation of the object.
	 * @return the process ID if it is not null, otherwise returns "???"
	 */
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

	/**
	 * Creates the parent directory for the specified file.
	 * @param file the file for which the parent directory needs to be created
	 */
	private void createParentDirectory(File file) {
		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
	}

	/**
	 * Asserts that the specified file can be overwritten.
	 * @param file the file to be checked
	 * @throws IOException if an I/O error occurs
	 * @throws FileNotFoundException if the file cannot be written or if the POSIX file
	 * permissions do not allow writing
	 */
	private void assertCanOverwrite(File file) throws IOException {
		if (!file.canWrite() || !canWritePosixFile(file)) {
			throw new FileNotFoundException(file + " (permission denied)");
		}
	}

	/**
	 * Checks if the specified file has write permissions for the current user.
	 * @param file the file to check
	 * @return true if the file has write permissions, false otherwise
	 * @throws IOException if an I/O error occurs while checking the file permissions
	 */
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
