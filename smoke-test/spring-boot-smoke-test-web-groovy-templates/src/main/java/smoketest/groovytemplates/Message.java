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

package smoketest.groovytemplates;

import java.util.Date;

import jakarta.validation.constraints.NotEmpty;
import org.jspecify.annotations.Nullable;

public class Message {

	private @Nullable Long id;

	@NotEmpty(message = "Text is required.")
	private @Nullable String text;

	@NotEmpty(message = "Summary is required.")
	private @Nullable String summary;

	private Date created = new Date();

	public @Nullable Long getId() {
		return this.id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	public Date getCreated() {
		return this.created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public @Nullable String getText() {
		return this.text;
	}

	public void setText(@Nullable String text) {
		this.text = text;
	}

	public @Nullable String getSummary() {
		return this.summary;
	}

	public void setSummary(@Nullable String summary) {
		this.summary = summary;
	}

}
