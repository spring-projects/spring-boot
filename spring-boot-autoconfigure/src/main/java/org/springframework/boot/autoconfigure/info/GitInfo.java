/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.info;

/**
 * Provide git-related information such as commit id and time.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class GitInfo {

	private String branch;

	private final Commit commit = new Commit();

	public String getBranch() {
		return this.branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public Commit getCommit() {
		return this.commit;
	}

	public static class Commit {

		private String id;

		private String time;

		public String getId() {
			return (this.id == null ? "" : getShortId(this.id));
		}

		private String getShortId(String string) {
			return string.substring(0, Math.min(this.id.length(), 7));
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTime() {
			return this.time;
		}

		public void setTime(String time) {
			this.time = time;
		}

	}

}
