/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.layout;

import java.io.File;

import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.LayoutFactory;

public class SampleLayoutFactory implements LayoutFactory {

	private String name = "sample";

	public SampleLayoutFactory() {
	}

	public SampleLayoutFactory(String name) {
		this.name = name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public Layout getLayout(File source) {
		return new SampleLayout(this.name);
	}

}
