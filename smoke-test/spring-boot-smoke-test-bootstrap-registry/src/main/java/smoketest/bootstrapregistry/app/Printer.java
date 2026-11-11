/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.app;

import smoketest.bootstrapregistry.external.svn.SubversionClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Printer {

	Printer(@Value("${svn}") String svn, SubversionClient subversionClient) {
		System.out.println("--- svn " + svn);
		System.out.println("--- client " + subversionClient.getClass().getName());
	}

}
