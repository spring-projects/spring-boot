/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jpa.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import org.jspecify.annotations.Nullable;

@Entity
public class Tag {

	@Id
	@SequenceGenerator(name = "tag_generator", sequenceName = "tag_sequence", initialValue = 4)
	@GeneratedValue(generator = "tag_generator")
	private @Nullable Long id;

	private @Nullable String name;

	@ManyToMany(mappedBy = "tags")
	private List<Note> notes = new ArrayList<>();

	public @Nullable Long getId() {
		return this.id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	public List<Note> getNotes() {
		return this.notes;
	}

	public void setNotes(List<Note> notes) {
		this.notes = notes;
	}

}
