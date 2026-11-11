/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.graphql;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

public class Project {

	private String slug;

	private String name;

	public Project(String slug, String name) {
		this.slug = slug;
		this.name = name;
	}

	public String getSlug() {
		return this.slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Project project = (Project) o;
		return this.slug.equals(project.slug);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.slug);
	}

}
