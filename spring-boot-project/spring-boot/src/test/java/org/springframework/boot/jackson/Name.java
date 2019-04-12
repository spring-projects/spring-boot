package org.springframework.boot.jackson;

public class Name {
	protected final String name;

	public Name(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
