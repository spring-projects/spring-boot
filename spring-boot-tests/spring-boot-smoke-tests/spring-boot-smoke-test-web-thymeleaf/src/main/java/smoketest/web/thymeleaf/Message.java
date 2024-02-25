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

package smoketest.web.thymeleaf;

import java.util.Calendar;

import jakarta.validation.constraints.NotEmpty;

/**
 * Message class.
 */
public class Message {

	private Long id;

	@NotEmpty(message = "Text is required.")
	private String text;

	@NotEmpty(message = "Summary is required.")
	private String summary;

	private Calendar created = Calendar.getInstance();

	/**
	 * Returns the ID of the message.
	 * @return the ID of the message
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the ID of the message.
	 * @param id the ID to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Returns the date and time when the message was created.
	 * @return the date and time when the message was created
	 */
	public Calendar getCreated() {
		return this.created;
	}

	/**
	 * Sets the created date and time of the message.
	 * @param created the Calendar object representing the created date and time
	 */
	public void setCreated(Calendar created) {
		this.created = created;
	}

	/**
	 * Returns the text of the message.
	 * @return the text of the message
	 */
	public String getText() {
		return this.text;
	}

	/**
	 * Sets the text of the message.
	 * @param text the text to be set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Returns the summary of the message.
	 * @return the summary of the message
	 */
	public String getSummary() {
		return this.summary;
	}

	/**
	 * Sets the summary of the message.
	 * @param summary the summary to be set
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

}
