/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;

/**
 * An {@link UpdateEvent} that includes progress information.
 *
 * @author Phillip Webb
 * @author Wolfgang Kronberg
 * @since 2.3.0
 */
public abstract class ProgressUpdateEvent extends UpdateEvent {

	private final @Nullable String status;

	private final @Nullable ProgressDetail progressDetail;

	private final @Nullable String progress;

	protected ProgressUpdateEvent(@Nullable String status, @Nullable ProgressDetail progressDetail,
			@Nullable String progress) {
		this.status = status;
		this.progressDetail = (ProgressDetail.isEmpty(progressDetail)) ? null : progressDetail;
		this.progress = progress;
	}

	/**
	 * Return the status for the update. For example, "Extracting" or "Downloading".
	 * @return the status of the update.
	 */
	public @Nullable String getStatus() {
		return this.status;
	}

	/**
	 * Return progress details if available.
	 * @return progress details or {@code null}
	 */
	public @Nullable ProgressDetail getProgressDetail() {
		return this.progressDetail;
	}

	/**
	 * Return a text based progress bar if progress information is available.
	 * @return the progress bar or {@code null}
	 */
	public @Nullable String getProgress() {
		return this.progress;
	}

	/**
	 * Provide details about the progress of a task.
	 */
	public static class ProgressDetail {

		private final @Nullable Long current;

		private final @Nullable Long total;

		@JsonCreator
		public ProgressDetail(@Nullable Long current, @Nullable Long total) {
			this.current = current;
			this.total = total;
		}

		/**
		 * Return the progress as a percentage.
		 * @return the progress percentage
		 * @since 3.3.7
		 */
		public int asPercentage() {
			if (this.total == null || this.current == null) {
				return 0;
			}
			int percentage = (int) ((100.0 / this.total) * this.current);
			return (percentage < 0) ? 0 : Math.min(percentage, 100);
		}

		private static boolean isEmpty(@Nullable ProgressDetail progressDetail) {
			return progressDetail == null || progressDetail.current == null || progressDetail.total == null;
		}

	}

}
