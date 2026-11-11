/*
 * Copyright 2012-present the original author or authors.
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
