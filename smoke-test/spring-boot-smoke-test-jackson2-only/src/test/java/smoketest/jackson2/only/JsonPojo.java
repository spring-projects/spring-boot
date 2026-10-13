/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.only;

/**
 * POJO for serialization to JSON.
 *
 * @author Andy Wilkinson
 */
class JsonPojo {

	private String alpha;

	private String bravo;

	String getAlpha() {
		return this.alpha;
	}

	void setAlpha(String alpha) {
		this.alpha = alpha;
	}

	String getBravo() {
		return this.bravo;
	}

	void setBravo(String bravo) {
		this.bravo = bravo;
	}

}
