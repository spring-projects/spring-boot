/*
 * Copyright 2012-2020 the original author or authors.
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

/**
 * A {@link ProgressUpdateEvent} fired as an image is pulled.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class PullImageUpdateEvent extends ProgressUpdateEvent {

	private final String id;

	@JsonCreator
	public PullImageUpdateEvent(String id, String status, ProgressDetail progressDetail, String progress) {
		super(status, progressDetail, progress);
		this.id = id;
	}

	/**
	 * Return the ID of the layer being updated if available.
	 * @return the ID of the updated layer or {@code null}
	 */
	public String getId() {
		return this.id;
	}

}
