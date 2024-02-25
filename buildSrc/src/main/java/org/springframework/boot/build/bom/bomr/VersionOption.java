/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.build.bom.bomr;

import java.util.List;

import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.util.StringUtils;

/**
 * An option for a library update.
 *
 * @author Andy Wilkinson
 */
class VersionOption {

	private final DependencyVersion version;

	/**
     * Constructor for VersionOption class.
     * 
     * @param version the dependency version to be set
     */
    VersionOption(DependencyVersion version) {
		this.version = version;
	}

	/**
     * Returns the version of the dependency.
     *
     * @return the version of the dependency
     */
    DependencyVersion getVersion() {
		return this.version;
	}

	/**
     * Returns a string representation of the VersionOption object.
     * 
     * @return the string representation of the VersionOption object
     */
    @Override
	public String toString() {
		return this.version.toString();
	}

	/**
     * AlignedVersionOption class.
     */
    static final class AlignedVersionOption extends VersionOption {

		private final VersionAlignment alignedWith;

		/**
         * Constructs a new AlignedVersionOption with the specified DependencyVersion and VersionAlignment.
         * 
         * @param version the DependencyVersion for this AlignedVersionOption
         * @param alignedWith the VersionAlignment to align this AlignedVersionOption with
         */
        AlignedVersionOption(DependencyVersion version, VersionAlignment alignedWith) {
			super(version);
			this.alignedWith = alignedWith;
		}

		/**
         * Returns a string representation of the AlignedVersionOption object.
         * 
         * @return a string representation of the AlignedVersionOption object, including the alignment information
         *         with another version
         */
        @Override
		public String toString() {
			return super.toString() + " (aligned with " + this.alignedWith + ")";
		}

	}

	/**
     * ResolvedVersionOption class.
     */
    static final class ResolvedVersionOption extends VersionOption {

		private final List<String> missingModules;

		/**
         * Constructs a new ResolvedVersionOption object with the specified dependency version and list of missing modules.
         * 
         * @param version the dependency version for this ResolvedVersionOption
         * @param missingModules the list of missing modules for this ResolvedVersionOption
         */
        ResolvedVersionOption(DependencyVersion version, List<String> missingModules) {
			super(version);
			this.missingModules = missingModules;
		}

		/**
         * Returns a string representation of the ResolvedVersionOption object.
         * If there are no missing modules, the string representation is the same as the default toString() method.
         * If there are missing modules, the string representation includes the missing modules in addition to the default toString() output.
         * 
         * @return a string representation of the ResolvedVersionOption object
         */
        @Override
		public String toString() {
			if (this.missingModules.isEmpty()) {
				return super.toString();
			}
			return super.toString() + " (some modules are missing: "
					+ StringUtils.collectionToDelimitedString(this.missingModules, ", ") + ")";
		}

	}

}
