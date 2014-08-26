package org.springframework.boot.autoconfigure.PackageAutoConfigureD;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;

/**
 * Test class for {@link AutoConfigurationSorter}.
 * 
 * @author David Liu
 * @since 1.1.4
 */

@AutoConfigureAfter(packages = "org.springframework.boot.autoconfigure.PackageAutoConfigureA")
public class PackageD1AutoConfiguration {

}
