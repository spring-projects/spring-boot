package org.springframework.boot.jackson;

/**
 * Sample object used for tests.
 *
 * @author Paul Aly
 */
public class NameAndCareer extends Name {

	private final String career;

	public NameAndCareer(String name, String career) {
		super(name);
		this.career = career;
	}

	public String getCareer() {
		return career;
	}

}
