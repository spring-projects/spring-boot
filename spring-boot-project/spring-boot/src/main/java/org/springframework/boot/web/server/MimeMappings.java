/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Simple server-independent abstraction for mime mappings. Roughly equivalent to the
 * {@literal &lt;mime-mapping&gt;} element traditionally found in web.xml.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class MimeMappings implements Iterable<MimeMappings.Mapping> {

	/**
	 * Default mime mapping commonly used.
	 */
	public static final MimeMappings DEFAULT;

	static {
		MimeMappings mappings = new MimeMappings();
		mappings.add("abs", "audio/x-mpeg");
		mappings.add("ai", "application/postscript");
		mappings.add("aif", "audio/x-aiff");
		mappings.add("aifc", "audio/x-aiff");
		mappings.add("aiff", "audio/x-aiff");
		mappings.add("aim", "application/x-aim");
		mappings.add("art", "image/x-jg");
		mappings.add("asf", "video/x-ms-asf");
		mappings.add("asx", "video/x-ms-asf");
		mappings.add("au", "audio/basic");
		mappings.add("avi", "video/x-msvideo");
		mappings.add("avx", "video/x-rad-screenplay");
		mappings.add("bcpio", "application/x-bcpio");
		mappings.add("bin", "application/octet-stream");
		mappings.add("bmp", "image/bmp");
		mappings.add("body", "text/html");
		mappings.add("cdf", "application/x-cdf");
		mappings.add("cer", "application/pkix-cert");
		mappings.add("class", "application/java");
		mappings.add("cpio", "application/x-cpio");
		mappings.add("csh", "application/x-csh");
		mappings.add("css", "text/css");
		mappings.add("dib", "image/bmp");
		mappings.add("doc", "application/msword");
		mappings.add("dtd", "application/xml-dtd");
		mappings.add("dv", "video/x-dv");
		mappings.add("dvi", "application/x-dvi");
		mappings.add("eot", "application/vnd.ms-fontobject");
		mappings.add("eps", "application/postscript");
		mappings.add("etx", "text/x-setext");
		mappings.add("exe", "application/octet-stream");
		mappings.add("gif", "image/gif");
		mappings.add("gtar", "application/x-gtar");
		mappings.add("gz", "application/x-gzip");
		mappings.add("hdf", "application/x-hdf");
		mappings.add("hqx", "application/mac-binhex40");
		mappings.add("htc", "text/x-component");
		mappings.add("htm", "text/html");
		mappings.add("html", "text/html");
		mappings.add("ief", "image/ief");
		mappings.add("jad", "text/vnd.sun.j2me.app-descriptor");
		mappings.add("jar", "application/java-archive");
		mappings.add("java", "text/x-java-source");
		mappings.add("jnlp", "application/x-java-jnlp-file");
		mappings.add("jpe", "image/jpeg");
		mappings.add("jpeg", "image/jpeg");
		mappings.add("jpg", "image/jpeg");
		mappings.add("js", "application/javascript");
		mappings.add("jsf", "text/plain");
		mappings.add("json", "application/json");
		mappings.add("jspf", "text/plain");
		mappings.add("kar", "audio/midi");
		mappings.add("latex", "application/x-latex");
		mappings.add("m3u", "audio/x-mpegurl");
		mappings.add("mac", "image/x-macpaint");
		mappings.add("man", "text/troff");
		mappings.add("mathml", "application/mathml+xml");
		mappings.add("me", "text/troff");
		mappings.add("mid", "audio/midi");
		mappings.add("midi", "audio/midi");
		mappings.add("mif", "application/x-mif");
		mappings.add("mov", "video/quicktime");
		mappings.add("movie", "video/x-sgi-movie");
		mappings.add("mp1", "audio/mpeg");
		mappings.add("mp2", "audio/mpeg");
		mappings.add("mp3", "audio/mpeg");
		mappings.add("mp4", "video/mp4");
		mappings.add("mpa", "audio/mpeg");
		mappings.add("mpe", "video/mpeg");
		mappings.add("mpeg", "video/mpeg");
		mappings.add("mpega", "audio/x-mpeg");
		mappings.add("mpg", "video/mpeg");
		mappings.add("mpv2", "video/mpeg2");
		mappings.add("ms", "application/x-wais-source");
		mappings.add("nc", "application/x-netcdf");
		mappings.add("oda", "application/oda");
		mappings.add("odb", "application/vnd.oasis.opendocument.database");
		mappings.add("odc", "application/vnd.oasis.opendocument.chart");
		mappings.add("odf", "application/vnd.oasis.opendocument.formula");
		mappings.add("odg", "application/vnd.oasis.opendocument.graphics");
		mappings.add("odi", "application/vnd.oasis.opendocument.image");
		mappings.add("odm", "application/vnd.oasis.opendocument.text-master");
		mappings.add("odp", "application/vnd.oasis.opendocument.presentation");
		mappings.add("ods", "application/vnd.oasis.opendocument.spreadsheet");
		mappings.add("odt", "application/vnd.oasis.opendocument.text");
		mappings.add("otg", "application/vnd.oasis.opendocument.graphics-template");
		mappings.add("oth", "application/vnd.oasis.opendocument.text-web");
		mappings.add("otp", "application/vnd.oasis.opendocument.presentation-template");
		mappings.add("ots", "application/vnd.oasis.opendocument.spreadsheet-template ");
		mappings.add("ott", "application/vnd.oasis.opendocument.text-template");
		mappings.add("ogx", "application/ogg");
		mappings.add("ogv", "video/ogg");
		mappings.add("oga", "audio/ogg");
		mappings.add("ogg", "audio/ogg");
		mappings.add("otf", "application/x-font-opentype");
		mappings.add("spx", "audio/ogg");
		mappings.add("flac", "audio/flac");
		mappings.add("anx", "application/annodex");
		mappings.add("axa", "audio/annodex");
		mappings.add("axv", "video/annodex");
		mappings.add("xspf", "application/xspf+xml");
		mappings.add("pbm", "image/x-portable-bitmap");
		mappings.add("pct", "image/pict");
		mappings.add("pdf", "application/pdf");
		mappings.add("pgm", "image/x-portable-graymap");
		mappings.add("pic", "image/pict");
		mappings.add("pict", "image/pict");
		mappings.add("pls", "audio/x-scpls");
		mappings.add("png", "image/png");
		mappings.add("pnm", "image/x-portable-anymap");
		mappings.add("pnt", "image/x-macpaint");
		mappings.add("ppm", "image/x-portable-pixmap");
		mappings.add("ppt", "application/vnd.ms-powerpoint");
		mappings.add("pps", "application/vnd.ms-powerpoint");
		mappings.add("ps", "application/postscript");
		mappings.add("psd", "image/vnd.adobe.photoshop");
		mappings.add("qt", "video/quicktime");
		mappings.add("qti", "image/x-quicktime");
		mappings.add("qtif", "image/x-quicktime");
		mappings.add("ras", "image/x-cmu-raster");
		mappings.add("rdf", "application/rdf+xml");
		mappings.add("rgb", "image/x-rgb");
		mappings.add("rm", "application/vnd.rn-realmedia");
		mappings.add("roff", "text/troff");
		mappings.add("rtf", "application/rtf");
		mappings.add("rtx", "text/richtext");
		mappings.add("sfnt", "application/font-sfnt");
		mappings.add("sh", "application/x-sh");
		mappings.add("shar", "application/x-shar");
		mappings.add("sit", "application/x-stuffit");
		mappings.add("snd", "audio/basic");
		mappings.add("src", "application/x-wais-source");
		mappings.add("sv4cpio", "application/x-sv4cpio");
		mappings.add("sv4crc", "application/x-sv4crc");
		mappings.add("svg", "image/svg+xml");
		mappings.add("svgz", "image/svg+xml");
		mappings.add("swf", "application/x-shockwave-flash");
		mappings.add("t", "text/troff");
		mappings.add("tar", "application/x-tar");
		mappings.add("tcl", "application/x-tcl");
		mappings.add("tex", "application/x-tex");
		mappings.add("texi", "application/x-texinfo");
		mappings.add("texinfo", "application/x-texinfo");
		mappings.add("tif", "image/tiff");
		mappings.add("tiff", "image/tiff");
		mappings.add("tr", "text/troff");
		mappings.add("tsv", "text/tab-separated-values");
		mappings.add("ttf", "application/x-font-ttf");
		mappings.add("txt", "text/plain");
		mappings.add("ulw", "audio/basic");
		mappings.add("ustar", "application/x-ustar");
		mappings.add("vxml", "application/voicexml+xml");
		mappings.add("xbm", "image/x-xbitmap");
		mappings.add("xht", "application/xhtml+xml");
		mappings.add("xhtml", "application/xhtml+xml");
		mappings.add("xls", "application/vnd.ms-excel");
		mappings.add("xml", "application/xml");
		mappings.add("xpm", "image/x-xpixmap");
		mappings.add("xsl", "application/xml");
		mappings.add("xslt", "application/xslt+xml");
		mappings.add("xul", "application/vnd.mozilla.xul+xml");
		mappings.add("xwd", "image/x-xwindowdump");
		mappings.add("vsd", "application/vnd.visio");
		mappings.add("wav", "audio/x-wav");
		mappings.add("wbmp", "image/vnd.wap.wbmp");
		mappings.add("wml", "text/vnd.wap.wml");
		mappings.add("wmlc", "application/vnd.wap.wmlc");
		mappings.add("wmls", "text/vnd.wap.wmlsc");
		mappings.add("wmlscriptc", "application/vnd.wap.wmlscriptc");
		mappings.add("wmv", "video/x-ms-wmv");
		mappings.add("woff", "application/font-woff");
		mappings.add("woff2", "application/font-woff2");
		mappings.add("wrl", "model/vrml");
		mappings.add("wspolicy", "application/wspolicy+xml");
		mappings.add("z", "application/x-compress");
		mappings.add("zip", "application/zip");
		DEFAULT = unmodifiableMappings(mappings);
	}

	private final Map<String, Mapping> map;

	/**
	 * Create a new empty {@link MimeMappings} instance.
	 */
	public MimeMappings() {
		this.map = new LinkedHashMap<>();
	}

	/**
	 * Create a new {@link MimeMappings} instance from the specified mappings.
	 * @param mappings the source mappings
	 */
	public MimeMappings(MimeMappings mappings) {
		this(mappings, true);
	}

	/**
	 * Create a new {@link MimeMappings} from the specified mappings.
	 * @param mappings the source mappings with extension as the key and mime-type as the
	 * value
	 */
	public MimeMappings(Map<String, String> mappings) {
		Assert.notNull(mappings, "Mappings must not be null");
		this.map = new LinkedHashMap<>();
		mappings.forEach(this::add);
	}

	/**
	 * Internal constructor.
	 * @param mappings source mappings
	 * @param mutable if the new object should be mutable.
	 */
	private MimeMappings(MimeMappings mappings, boolean mutable) {
		Assert.notNull(mappings, "Mappings must not be null");
		this.map = (mutable ? new LinkedHashMap<>(mappings.map)
				: Collections.unmodifiableMap(mappings.map));
	}

	@Override
	public Iterator<Mapping> iterator() {
		return getAll().iterator();
	}

	/**
	 * Returns all defined mappings.
	 * @return the mappings.
	 */
	public Collection<Mapping> getAll() {
		return this.map.values();
	}

	/**
	 * Add a new mime mapping.
	 * @param extension the file extension (excluding '.')
	 * @param mimeType the mime type to map
	 * @return any previous mapping or {@code null}
	 */
	public String add(String extension, String mimeType) {
		Mapping previous = this.map.put(extension, new Mapping(extension, mimeType));
		return (previous != null) ? previous.getMimeType() : null;
	}

	/**
	 * Get a mime mapping for the given extension.
	 * @param extension the file extension (excluding '.')
	 * @return a mime mapping or {@code null}
	 */
	public String get(String extension) {
		Mapping mapping = this.map.get(extension);
		return (mapping != null) ? mapping.getMimeType() : null;
	}

	/**
	 * Remove an existing mapping.
	 * @param extension the file extension (excluding '.')
	 * @return the removed mime mapping or {@code null} if no item was removed
	 */
	public String remove(String extension) {
		Mapping previous = this.map.remove(extension);
		return (previous != null) ? previous.getMimeType() : null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof MimeMappings) {
			MimeMappings other = (MimeMappings) obj;
			return this.map.equals(other.map);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.map.hashCode();
	}

	/**
	 * Create a new unmodifiable view of the specified mapping. Methods that attempt to
	 * modify the returned map will throw {@link UnsupportedOperationException}s.
	 * @param mappings the mappings
	 * @return an unmodifiable view of the specified mappings.
	 */
	public static MimeMappings unmodifiableMappings(MimeMappings mappings) {
		return new MimeMappings(mappings, false);
	}

	/**
	 * A single mime mapping.
	 */
	public static final class Mapping {

		private final String extension;

		private final String mimeType;

		public Mapping(String extension, String mimeType) {
			Assert.notNull(extension, "Extension must not be null");
			Assert.notNull(mimeType, "MimeType must not be null");
			this.extension = extension;
			this.mimeType = mimeType;
		}

		public String getExtension() {
			return this.extension;
		}

		public String getMimeType() {
			return this.mimeType;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (obj instanceof Mapping) {
				Mapping other = (Mapping) obj;
				return this.extension.equals(other.extension)
						&& this.mimeType.equals(other.mimeType);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.extension.hashCode();
		}

		@Override
		public String toString() {
			return "Mapping [extension=" + this.extension + ", mimeType=" + this.mimeType
					+ "]";
		}

	}

}
