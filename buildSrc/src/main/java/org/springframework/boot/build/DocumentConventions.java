/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.resources.TextResourceFactory;

import org.springframework.util.FileCopyUtils;

class DocumentConventions {

	void copyLegalFiles(Project project, CopySpec metaInf) {
		copyNoticeFile(project, metaInf);
		copyLicenseFile(project, metaInf);
	}

	void copyNoticeFile(Project project, CopySpec metaInf) {
		try {
			InputStream notice = getClass().getClassLoader().getResourceAsStream("NOTICE.txt");
			String noticeContent = FileCopyUtils.copyToString(new InputStreamReader(notice, StandardCharsets.UTF_8))
					.replace("${version}", project.getVersion().toString());
			TextResourceFactory resourceFactory = project.getResources().getText();
			File file = createLegalFile(resourceFactory.fromString(noticeContent).asFile(), "NOTICE.txt");
			metaInf.from(file);
		}
		catch (IOException ex) {
			throw new GradleException("Failed to copy NOTICE.txt", ex);
		}
	}

	void copyLicenseFile(Project project, CopySpec metaInf) {
		URL license = getClass().getClassLoader().getResource("LICENSE.txt");
		try {
			TextResourceFactory resourceFactory = project.getResources().getText();
			File file = createLegalFile(resourceFactory.fromUri(license.toURI()).asFile(), "LICENSE.txt");
			metaInf.from(file);
		}
		catch (URISyntaxException ex) {
			throw new GradleException("Failed to copy LICENSE.txt", ex);
		}
	}

	File createLegalFile(File source, String filename) {
		File legalFile = new File(source.getParentFile(), filename);
		source.renameTo(legalFile);
		return legalFile;
	}

}
