package org.springframework.boot.autoconfigure.PackageAutoConfigureZ;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;

/**
 * Test class for {@link AutoConfigurationSorter}.
 * 
 * @author David Liu
 * @since 1.1.4
 */

@AutoConfigureBefore(packages = "org.springframework.boot.autoconfigure.PackageAutoConfigureY")
public class PackageZ1AutoConfiguration {

}
