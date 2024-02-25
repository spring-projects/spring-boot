/*
 * Copyright 2012-2021 the original author or authors.
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

package smoketest.jpa.domain;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;

/**
 * Note class.
 */
@Entity
public class Note {

	@Id
	@SequenceGenerator(name = "note_generator", sequenceName = "note_sequence", initialValue = 5)
	@GeneratedValue(generator = "note_generator")
	private long id;

	private String title;

	private String body;

	@ManyToMany
	private List<Tag> tags;

	/**
	 * Returns the ID of the Note.
	 * @return the ID of the Note
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * Sets the ID of the note.
	 * @param id the ID to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Returns the title of the note.
	 * @return the title of the note
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Sets the title of the note.
	 * @param title the new title for the note
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the body of the note.
	 * @return the body of the note
	 */
	public String getBody() {
		return this.body;
	}

	/**
	 * Sets the body of the note.
	 * @param body the body of the note to be set
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * Returns the list of tags associated with the note.
	 * @return the list of tags
	 */
	public List<Tag> getTags() {
		return this.tags;
	}

	/**
	 * Sets the list of tags for the note.
	 * @param tags the list of tags to be set
	 */
	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

}
