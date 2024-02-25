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
 * Tag class.
 */
@Entity
public class Tag {

	@Id
	@SequenceGenerator(name = "tag_generator", sequenceName = "tag_sequence", initialValue = 4)
	@GeneratedValue(generator = "tag_generator")
	private long id;

	private String name;

	@ManyToMany(mappedBy = "tags")
	private List<Note> notes;

	/**
     * Returns the ID of the Tag.
     *
     * @return the ID of the Tag
     */
    public long getId() {
		return this.id;
	}

	/**
     * Sets the ID of the Tag.
     * 
     * @param id the ID to set
     */
    public void setId(long id) {
		this.id = id;
	}

	/**
     * Returns the name of the Tag.
     *
     * @return the name of the Tag
     */
    public String getName() {
		return this.name;
	}

	/**
     * Sets the name of the Tag.
     * 
     * @param name the name to be set
     */
    public void setName(String name) {
		this.name = name;
	}

	/**
     * Returns the list of notes.
     *
     * @return the list of notes
     */
    public List<Note> getNotes() {
		return this.notes;
	}

	/**
     * Sets the list of notes for this tag.
     * 
     * @param notes the list of notes to be set
     */
    public void setNotes(List<Note> notes) {
		this.notes = notes;
	}

}
