/*
 * Copyright 2012-2017 the original author or authors.
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

/**
 * System that allows self-contained JAR/WAR archives to be launched using
 * {@code java -jar}. Archives can include nested packaged dependency JARs (there is no
 * need to create shade style jars) and are executed without unpacking. The only
 * constraint is that nested JARs must be stored in the archive uncompressed.
 *
 * @see org.springframework.boot.loader.JarLauncher
 * @see org.springframework.boot.loader.WarLauncher
 */
package org.springframework.boot.loader;
