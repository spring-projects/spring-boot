/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

public class ManifestReader {

	/**
	 * Class logger
	 */
	private final Log log = LogFactory.getLog(getClass());
	
	/**
     * Path to stored manifest files.
     */
    private static final String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";
    
	private static final String IMPLEMENTATION_VERSION = "Implementation-Version";

	private static final String IMPLEMENTATION_TITLE = "Implementation-Title";

    public Map<String, String> getVersions() {
    	
    	Map<String, String> result = new TreeMap<String, String>();

        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources(META_INF_MANIFEST_MF);
            while (resources.hasMoreElements()) {
            	String urlName = null;
            	try {
	            	URL url = resources.nextElement();
	            	urlName = url.toString();
	            	if (!urlName.contains("org/springframework/")) {
		        		continue;
		        	}
	            	if (urlName.contains("org/springframework/boot/")) {
	            		// skipping since starter versions are usually same (as SpringBoot)
		        		continue;
		        	}
	            	Manifest manifest = new Manifest(url.openStream());
	            	String title = manifest.getMainAttributes().getValue(IMPLEMENTATION_TITLE);
	            	if (title == null) {
	            		title = parseLibraryNameFromUrl(urlName);
	            		if (title == null) {
	            			// when version is not even in the URL
	            			if (log.isTraceEnabled()) {
	            				log.trace(String.format("Version is not parsable from this URL: ", urlName));
	            			}
	            			continue;
	            		}
	            		result.put(title, title);
	            	} else {
	            		String version = manifest.getMainAttributes().getValue(IMPLEMENTATION_VERSION);
	            		result.put(title, String.format("%s %s", title, getValue(version)));
	            	}
            	} catch (IOException ioEx) {
            		log.warn(String.format("Manifest is not readable (%s)!", urlName));
            	}
            }            
        }
        catch (IOException ex) {
        	log.error("MANIFEST.MF reading error: " + ex.getMessage());
        	return result;
        }
        return result;
    }	

    /**
     * Helper method to get attribute's value.
     *
     * @param value given value
     * @return value "N/A" when value is <code>null</code> otherwise value itself.
     */
    private String getValue(String value) {
        return StringUtils.isEmpty(value) ? "N/A" : String.format("v%s", value);
    }

    private String parseLibraryNameFromUrl(String url) {
        int lastIndex = url.indexOf("!");
        if (lastIndex < 0) {
        	// URL is not for JAR
        	return null;
        }
        int firstIndex = url.substring(0, lastIndex).lastIndexOf("/");
        		
    	return firstIndex < 0 ? url.substring(0, lastIndex) : url.substring(firstIndex + 1, lastIndex);
    }

}
