/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.applicationarguments;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	public MyBean(ApplicationArguments args) {
		boolean debug = args.containsOption("debug");
		List<String> files = args.getNonOptionArgs();
		if (debug) {
			System.out.println(files);
		}
		// if run with "--debug logfile.txt" prints ["logfile.txt"]
	}

}
