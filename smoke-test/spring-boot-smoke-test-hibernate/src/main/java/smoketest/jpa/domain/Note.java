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
public class Note {

	@Id
	@SequenceGenerator(name = "note_generator", sequenceName = "note_sequence", initialValue = 5)
	@GeneratedValue(generator = "note_generator")
	private @Nullable Long id;

	private @Nullable String title;

	private @Nullable String body;

	@ManyToMany
	private List<Tag> tags = new ArrayList<>();

	public @Nullable Long getId() {
		return this.id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	public @Nullable String getTitle() {
		return this.title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	public @Nullable String getBody() {
		return this.body;
	}

	public void setBody(@Nullable String body) {
		this.body = body;
	}

	public List<Tag> getTags() {
		return this.tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

}
