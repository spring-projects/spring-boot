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

package org.springframework.boot.build.bom.bomr;

import java.util.List;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.util.StringUtils;

/**
 * An option for a library update.
 *
 * @author Andy Wilkinson
 */
class VersionOption {

	private final DependencyVersion version;

	VersionOption(DependencyVersion version) {
		this.version = version;
	}

	DependencyVersion getVersion() {
		return this.version;
	}

	@Override
	public String toString() {
		return this.version.toString();
	}

	static final class AlignedVersionOption extends VersionOption {

		private final Library alignedWith;

		AlignedVersionOption(DependencyVersion version, Library alignedWith) {
			super(version);
			this.alignedWith = alignedWith;
		}

		@Override
		public String toString() {
			return super.toString() + " (aligned with " + this.alignedWith.getName() + " "
					+ this.alignedWith.getVersion().getVersion() + ")";
		}

	}

	static final class ResolvedVersionOption extends VersionOption {

		private final List<String> missingModules;

		ResolvedVersionOption(DependencyVersion version, List<String> missingModules) {
			super(version);
			this.missingModules = missingModules;
		}

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
