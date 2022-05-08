/*
 * Copyright 2012-2022 the original author or authors.
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
		mappings.add("123", "application/vnd.lotus-1-2-3");
		mappings.add("3dml", "text/vnd.in3d.3dml");
		mappings.add("3ds", "image/x-3ds");
		mappings.add("3g2", "video/3gpp2");
		mappings.add("3gp", "video/3gpp");
		mappings.add("7z", "application/x-7z-compressed");
		mappings.add("aab", "application/x-authorware-bin");
		mappings.add("aac", "audio/x-aac");
		mappings.add("aam", "application/x-authorware-map");
		mappings.add("aas", "application/x-authorware-seg");
		mappings.add("abs", "audio/x-mpeg");
		mappings.add("abw", "application/x-abiword");
		mappings.add("ac", "application/pkix-attr-cert");
		mappings.add("acc", "application/vnd.americandynamics.acc");
		mappings.add("ace", "application/x-ace-compressed");
		mappings.add("acu", "application/vnd.acucobol");
		mappings.add("acutc", "application/vnd.acucorp");
		mappings.add("adp", "audio/adpcm");
		mappings.add("aep", "application/vnd.audiograph");
		mappings.add("afm", "application/x-font-type1");
		mappings.add("afp", "application/vnd.ibm.modcap");
		mappings.add("ahead", "application/vnd.ahead.space");
		mappings.add("ai", "application/postscript");
		mappings.add("aif", "audio/x-aiff");
		mappings.add("aifc", "audio/x-aiff");
		mappings.add("aiff", "audio/x-aiff");
		mappings.add("aim", "application/x-aim");
		mappings.add("air", "application/vnd.adobe.air-application-installer-package+zip");
		mappings.add("ait", "application/vnd.dvb.ait");
		mappings.add("ami", "application/vnd.amiga.ami");
		mappings.add("anx", "application/annodex");
		mappings.add("apk", "application/vnd.android.package-archive");
		mappings.add("appcache", "text/cache-manifest");
		mappings.add("application", "application/x-ms-application");
		mappings.add("apr", "application/vnd.lotus-approach");
		mappings.add("arc", "application/x-freearc");
		mappings.add("art", "image/x-jg");
		mappings.add("asc", "application/pgp-signature");
		mappings.add("asf", "video/x-ms-asf");
		mappings.add("asm", "text/x-asm");
		mappings.add("aso", "application/vnd.accpac.simply.aso");
		mappings.add("asx", "video/x-ms-asf");
		mappings.add("atc", "application/vnd.acucorp");
		mappings.add("atom", "application/atom+xml");
		mappings.add("atomcat", "application/atomcat+xml");
		mappings.add("atomsvc", "application/atomsvc+xml");
		mappings.add("atx", "application/vnd.antix.game-component");
		mappings.add("au", "audio/basic");
		mappings.add("avi", "video/x-msvideo");
		mappings.add("avx", "video/x-rad-screenplay");
		mappings.add("aw", "application/applixware");
		mappings.add("axa", "audio/annodex");
		mappings.add("axv", "video/annodex");
		mappings.add("azf", "application/vnd.airzip.filesecure.azf");
		mappings.add("azs", "application/vnd.airzip.filesecure.azs");
		mappings.add("azw", "application/vnd.amazon.ebook");
		mappings.add("bat", "application/x-msdownload");
		mappings.add("bcpio", "application/x-bcpio");
		mappings.add("bdf", "application/x-font-bdf");
		mappings.add("bdm", "application/vnd.syncml.dm+wbxml");
		mappings.add("bed", "application/vnd.realvnc.bed");
		mappings.add("bh2", "application/vnd.fujitsu.oasysprs");
		mappings.add("bin", "application/octet-stream");
		mappings.add("blb", "application/x-blorb");
		mappings.add("blorb", "application/x-blorb");
		mappings.add("bmi", "application/vnd.bmi");
		mappings.add("bmp", "image/bmp");
		mappings.add("body", "text/html");
		mappings.add("book", "application/vnd.framemaker");
		mappings.add("box", "application/vnd.previewsystems.box");
		mappings.add("boz", "application/x-bzip2");
		mappings.add("bpk", "application/octet-stream");
		mappings.add("btif", "image/prs.btif");
		mappings.add("bz", "application/x-bzip");
		mappings.add("bz2", "application/x-bzip2");
		mappings.add("c", "text/x-c");
		mappings.add("c11amc", "application/vnd.cluetrust.cartomobile-config");
		mappings.add("c11amz", "application/vnd.cluetrust.cartomobile-config-pkg");
		mappings.add("c4d", "application/vnd.clonk.c4group");
		mappings.add("c4f", "application/vnd.clonk.c4group");
		mappings.add("c4g", "application/vnd.clonk.c4group");
		mappings.add("c4p", "application/vnd.clonk.c4group");
		mappings.add("c4u", "application/vnd.clonk.c4group");
		mappings.add("cab", "application/vnd.ms-cab-compressed");
		mappings.add("caf", "audio/x-caf");
		mappings.add("cap", "application/vnd.tcpdump.pcap");
		mappings.add("car", "application/vnd.curl.car");
		mappings.add("cat", "application/vnd.ms-pki.seccat");
		mappings.add("cb7", "application/x-cbr");
		mappings.add("cba", "application/x-cbr");
		mappings.add("cbr", "application/x-cbr");
		mappings.add("cbt", "application/x-cbr");
		mappings.add("cbz", "application/x-cbr");
		mappings.add("cc", "text/x-c");
		mappings.add("cct", "application/x-director");
		mappings.add("ccxml", "application/ccxml+xml");
		mappings.add("cdbcmsg", "application/vnd.contact.cmsg");
		mappings.add("cdf", "application/x-cdf");
		mappings.add("cdkey", "application/vnd.mediastation.cdkey");
		mappings.add("cdmia", "application/cdmi-capability");
		mappings.add("cdmic", "application/cdmi-container");
		mappings.add("cdmid", "application/cdmi-domain");
		mappings.add("cdmio", "application/cdmi-object");
		mappings.add("cdmiq", "application/cdmi-queue");
		mappings.add("cdx", "chemical/x-cdx");
		mappings.add("cdxml", "application/vnd.chemdraw+xml");
		mappings.add("cdy", "application/vnd.cinderella");
		mappings.add("cer", "application/pkix-cert");
		mappings.add("cfs", "application/x-cfs-compressed");
		mappings.add("cgm", "image/cgm");
		mappings.add("chat", "application/x-chat");
		mappings.add("chm", "application/vnd.ms-htmlhelp");
		mappings.add("chrt", "application/vnd.kde.kchart");
		mappings.add("cif", "chemical/x-cif");
		mappings.add("cii", "application/vnd.anser-web-certificate-issue-initiation");
		mappings.add("cil", "application/vnd.ms-artgalry");
		mappings.add("cla", "application/vnd.claymore");
		mappings.add("class", "application/java");
		mappings.add("clkk", "application/vnd.crick.clicker.keyboard");
		mappings.add("clkp", "application/vnd.crick.clicker.palette");
		mappings.add("clkt", "application/vnd.crick.clicker.template");
		mappings.add("clkw", "application/vnd.crick.clicker.wordbank");
		mappings.add("clkx", "application/vnd.crick.clicker");
		mappings.add("clp", "application/x-msclip");
		mappings.add("cmc", "application/vnd.cosmocaller");
		mappings.add("cmdf", "chemical/x-cmdf");
		mappings.add("cml", "chemical/x-cml");
		mappings.add("cmp", "application/vnd.yellowriver-custom-menu");
		mappings.add("cmx", "image/x-cmx");
		mappings.add("cod", "application/vnd.rim.cod");
		mappings.add("com", "application/x-msdownload");
		mappings.add("conf", "text/plain");
		mappings.add("cpio", "application/x-cpio");
		mappings.add("cpp", "text/x-c");
		mappings.add("cpt", "application/mac-compactpro");
		mappings.add("crd", "application/x-mscardfile");
		mappings.add("crl", "application/pkix-crl");
		mappings.add("crt", "application/x-x509-ca-cert");
		mappings.add("cryptonote", "application/vnd.rig.cryptonote");
		mappings.add("csh", "application/x-csh");
		mappings.add("csml", "chemical/x-csml");
		mappings.add("csp", "application/vnd.commonspace");
		mappings.add("css", "text/css");
		mappings.add("cst", "application/x-director");
		mappings.add("csv", "text/csv");
		mappings.add("cu", "application/cu-seeme");
		mappings.add("curl", "text/vnd.curl");
		mappings.add("cww", "application/prs.cww");
		mappings.add("cxt", "application/x-director");
		mappings.add("cxx", "text/x-c");
		mappings.add("dae", "model/vnd.collada+xml");
		mappings.add("daf", "application/vnd.mobius.daf");
		mappings.add("dart", "application/vnd.dart");
		mappings.add("dataless", "application/vnd.fdsn.seed");
		mappings.add("davmount", "application/davmount+xml");
		mappings.add("dbk", "application/docbook+xml");
		mappings.add("dcr", "application/x-director");
		mappings.add("dcurl", "text/vnd.curl.dcurl");
		mappings.add("dd2", "application/vnd.oma.dd2+xml");
		mappings.add("ddd", "application/vnd.fujixerox.ddd");
		mappings.add("deb", "application/x-debian-package");
		mappings.add("def", "text/plain");
		mappings.add("deploy", "application/octet-stream");
		mappings.add("der", "application/x-x509-ca-cert");
		mappings.add("dfac", "application/vnd.dreamfactory");
		mappings.add("dgc", "application/x-dgc-compressed");
		mappings.add("dib", "image/bmp");
		mappings.add("dic", "text/x-c");
		mappings.add("dir", "application/x-director");
		mappings.add("dis", "application/vnd.mobius.dis");
		mappings.add("dist", "application/octet-stream");
		mappings.add("distz", "application/octet-stream");
		mappings.add("djv", "image/vnd.djvu");
		mappings.add("djvu", "image/vnd.djvu");
		mappings.add("dll", "application/x-msdownload");
		mappings.add("dmg", "application/x-apple-diskimage");
		mappings.add("dmp", "application/vnd.tcpdump.pcap");
		mappings.add("dms", "application/octet-stream");
		mappings.add("dna", "application/vnd.dna");
		mappings.add("doc", "application/msword");
		mappings.add("docm", "application/vnd.ms-word.document.macroenabled.12");
		mappings.add("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		mappings.add("dot", "application/msword");
		mappings.add("dotm", "application/vnd.ms-word.template.macroenabled.12");
		mappings.add("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
		mappings.add("dp", "application/vnd.osgi.dp");
		mappings.add("dpg", "application/vnd.dpgraph");
		mappings.add("dra", "audio/vnd.dra");
		mappings.add("dsc", "text/prs.lines.tag");
		mappings.add("dssc", "application/dssc+der");
		mappings.add("dtb", "application/x-dtbook+xml");
		mappings.add("dtd", "application/xml-dtd");
		mappings.add("dts", "audio/vnd.dts");
		mappings.add("dtshd", "audio/vnd.dts.hd");
		mappings.add("dump", "application/octet-stream");
		mappings.add("dv", "video/x-dv");
		mappings.add("dvb", "video/vnd.dvb.file");
		mappings.add("dvi", "application/x-dvi");
		mappings.add("dwf", "model/vnd.dwf");
		mappings.add("dwg", "image/vnd.dwg");
		mappings.add("dxf", "image/vnd.dxf");
		mappings.add("dxp", "application/vnd.spotfire.dxp");
		mappings.add("dxr", "application/x-director");
		mappings.add("ecelp4800", "audio/vnd.nuera.ecelp4800");
		mappings.add("ecelp7470", "audio/vnd.nuera.ecelp7470");
		mappings.add("ecelp9600", "audio/vnd.nuera.ecelp9600");
		mappings.add("ecma", "application/ecmascript");
		mappings.add("edm", "application/vnd.novadigm.edm");
		mappings.add("edx", "application/vnd.novadigm.edx");
		mappings.add("efif", "application/vnd.picsel");
		mappings.add("ei6", "application/vnd.pg.osasli");
		mappings.add("elc", "application/octet-stream");
		mappings.add("emf", "application/x-msmetafile");
		mappings.add("eml", "message/rfc822");
		mappings.add("emma", "application/emma+xml");
		mappings.add("emz", "application/x-msmetafile");
		mappings.add("eol", "audio/vnd.digital-winds");
		mappings.add("eot", "application/vnd.ms-fontobject");
		mappings.add("eps", "application/postscript");
		mappings.add("epub", "application/epub+zip");
		mappings.add("es3", "application/vnd.eszigno3+xml");
		mappings.add("esa", "application/vnd.osgi.subsystem");
		mappings.add("esf", "application/vnd.epson.esf");
		mappings.add("et3", "application/vnd.eszigno3+xml");
		mappings.add("etx", "text/x-setext");
		mappings.add("eva", "application/x-eva");
		mappings.add("evy", "application/x-envoy");
		mappings.add("exe", "application/octet-stream");
		mappings.add("exi", "application/exi");
		mappings.add("ext", "application/vnd.novadigm.ext");
		mappings.add("ez", "application/andrew-inset");
		mappings.add("ez2", "application/vnd.ezpix-album");
		mappings.add("ez3", "application/vnd.ezpix-package");
		mappings.add("f", "text/x-fortran");
		mappings.add("f4v", "video/x-f4v");
		mappings.add("f77", "text/x-fortran");
		mappings.add("f90", "text/x-fortran");
		mappings.add("fbs", "image/vnd.fastbidsheet");
		mappings.add("fcdt", "application/vnd.adobe.formscentral.fcdt");
		mappings.add("fcs", "application/vnd.isac.fcs");
		mappings.add("fdf", "application/vnd.fdf");
		mappings.add("fe_launch", "application/vnd.denovo.fcselayout-link");
		mappings.add("fg5", "application/vnd.fujitsu.oasysgp");
		mappings.add("fgd", "application/x-director");
		mappings.add("fh", "image/x-freehand");
		mappings.add("fh4", "image/x-freehand");
		mappings.add("fh5", "image/x-freehand");
		mappings.add("fh7", "image/x-freehand");
		mappings.add("fhc", "image/x-freehand");
		mappings.add("fig", "application/x-xfig");
		mappings.add("flac", "audio/flac");
		mappings.add("fli", "video/x-fli");
		mappings.add("flo", "application/vnd.micrografx.flo");
		mappings.add("flv", "video/x-flv");
		mappings.add("flw", "application/vnd.kde.kivio");
		mappings.add("flx", "text/vnd.fmi.flexstor");
		mappings.add("fly", "text/vnd.fly");
		mappings.add("fm", "application/vnd.framemaker");
		mappings.add("fnc", "application/vnd.frogans.fnc");
		mappings.add("for", "text/x-fortran");
		mappings.add("fpx", "image/vnd.fpx");
		mappings.add("frame", "application/vnd.framemaker");
		mappings.add("fsc", "application/vnd.fsc.weblaunch");
		mappings.add("fst", "image/vnd.fst");
		mappings.add("ftc", "application/vnd.fluxtime.clip");
		mappings.add("fti", "application/vnd.anser-web-funds-transfer-initiation");
		mappings.add("fvt", "video/vnd.fvt");
		mappings.add("fxp", "application/vnd.adobe.fxp");
		mappings.add("fxpl", "application/vnd.adobe.fxp");
		mappings.add("fzs", "application/vnd.fuzzysheet");
		mappings.add("g2w", "application/vnd.geoplan");
		mappings.add("g3", "image/g3fax");
		mappings.add("g3w", "application/vnd.geospace");
		mappings.add("gac", "application/vnd.groove-account");
		mappings.add("gam", "application/x-tads");
		mappings.add("gbr", "application/rpki-ghostbusters");
		mappings.add("gca", "application/x-gca-compressed");
		mappings.add("gdl", "model/vnd.gdl");
		mappings.add("geo", "application/vnd.dynageo");
		mappings.add("gex", "application/vnd.geometry-explorer");
		mappings.add("ggb", "application/vnd.geogebra.file");
		mappings.add("ggt", "application/vnd.geogebra.tool");
		mappings.add("ghf", "application/vnd.groove-help");
		mappings.add("gif", "image/gif");
		mappings.add("gim", "application/vnd.groove-identity-message");
		mappings.add("gml", "application/gml+xml");
		mappings.add("gmx", "application/vnd.gmx");
		mappings.add("gnumeric", "application/x-gnumeric");
		mappings.add("gph", "application/vnd.flographit");
		mappings.add("gpx", "application/gpx+xml");
		mappings.add("gqf", "application/vnd.grafeq");
		mappings.add("gqs", "application/vnd.grafeq");
		mappings.add("gram", "application/srgs");
		mappings.add("gramps", "application/x-gramps-xml");
		mappings.add("gre", "application/vnd.geometry-explorer");
		mappings.add("grv", "application/vnd.groove-injector");
		mappings.add("grxml", "application/srgs+xml");
		mappings.add("gsf", "application/x-font-ghostscript");
		mappings.add("gtar", "application/x-gtar");
		mappings.add("gtm", "application/vnd.groove-tool-message");
		mappings.add("gtw", "model/vnd.gtw");
		mappings.add("gv", "text/vnd.graphviz");
		mappings.add("gxf", "application/gxf");
		mappings.add("gxt", "application/vnd.geonext");
		mappings.add("gz", "application/x-gzip");
		mappings.add("h", "text/x-c");
		mappings.add("h261", "video/h261");
		mappings.add("h263", "video/h263");
		mappings.add("h264", "video/h264");
		mappings.add("hal", "application/vnd.hal+xml");
		mappings.add("hbci", "application/vnd.hbci");
		mappings.add("hdf", "application/x-hdf");
		mappings.add("hh", "text/x-c");
		mappings.add("hlp", "application/winhlp");
		mappings.add("hpgl", "application/vnd.hp-hpgl");
		mappings.add("hpid", "application/vnd.hp-hpid");
		mappings.add("hps", "application/vnd.hp-hps");
		mappings.add("hqx", "application/mac-binhex40");
		mappings.add("htc", "text/x-component");
		mappings.add("htke", "application/vnd.kenameaapp");
		mappings.add("htm", "text/html");
		mappings.add("html", "text/html");
		mappings.add("hvd", "application/vnd.yamaha.hv-dic");
		mappings.add("hvp", "application/vnd.yamaha.hv-voice");
		mappings.add("hvs", "application/vnd.yamaha.hv-script");
		mappings.add("i2g", "application/vnd.intergeo");
		mappings.add("icc", "application/vnd.iccprofile");
		mappings.add("ice", "x-conference/x-cooltalk");
		mappings.add("icm", "application/vnd.iccprofile");
		mappings.add("ico", "image/x-icon");
		mappings.add("ics", "text/calendar");
		mappings.add("ief", "image/ief");
		mappings.add("ifb", "text/calendar");
		mappings.add("ifm", "application/vnd.shana.informed.formdata");
		mappings.add("iges", "model/iges");
		mappings.add("igl", "application/vnd.igloader");
		mappings.add("igm", "application/vnd.insors.igm");
		mappings.add("igs", "model/iges");
		mappings.add("igx", "application/vnd.micrografx.igx");
		mappings.add("iif", "application/vnd.shana.informed.interchange");
		mappings.add("imp", "application/vnd.accpac.simply.imp");
		mappings.add("ims", "application/vnd.ms-ims");
		mappings.add("in", "text/plain");
		mappings.add("ink", "application/inkml+xml");
		mappings.add("inkml", "application/inkml+xml");
		mappings.add("install", "application/x-install-instructions");
		mappings.add("iota", "application/vnd.astraea-software.iota");
		mappings.add("ipfix", "application/ipfix");
		mappings.add("ipk", "application/vnd.shana.informed.package");
		mappings.add("irm", "application/vnd.ibm.rights-management");
		mappings.add("irp", "application/vnd.irepository.package+xml");
		mappings.add("iso", "application/x-iso9660-image");
		mappings.add("itp", "application/vnd.shana.informed.formtemplate");
		mappings.add("ivp", "application/vnd.immervision-ivp");
		mappings.add("ivu", "application/vnd.immervision-ivu");
		mappings.add("jad", "text/vnd.sun.j2me.app-descriptor");
		mappings.add("jam", "application/vnd.jam");
		mappings.add("jar", "application/java-archive");
		mappings.add("java", "text/x-java-source");
		mappings.add("jisp", "application/vnd.jisp");
		mappings.add("jlt", "application/vnd.hp-jlyt");
		mappings.add("jnlp", "application/x-java-jnlp-file");
		mappings.add("joda", "application/vnd.joost.joda-archive");
		mappings.add("jpe", "image/jpeg");
		mappings.add("jpeg", "image/jpeg");
		mappings.add("jpg", "image/jpeg");
		mappings.add("jpgm", "video/jpm");
		mappings.add("jpgv", "video/jpeg");
		mappings.add("jpm", "video/jpm");
		mappings.add("js", "application/javascript");
		mappings.add("jsf", "text/plain");
		mappings.add("json", "application/json");
		mappings.add("jsonml", "application/jsonml+json");
		mappings.add("jspf", "text/plain");
		mappings.add("kar", "audio/midi");
		mappings.add("karbon", "application/vnd.kde.karbon");
		mappings.add("kfo", "application/vnd.kde.kformula");
		mappings.add("kia", "application/vnd.kidspiration");
		mappings.add("kml", "application/vnd.google-earth.kml+xml");
		mappings.add("kmz", "application/vnd.google-earth.kmz");
		mappings.add("kne", "application/vnd.kinar");
		mappings.add("knp", "application/vnd.kinar");
		mappings.add("kon", "application/vnd.kde.kontour");
		mappings.add("kpr", "application/vnd.kde.kpresenter");
		mappings.add("kpt", "application/vnd.kde.kpresenter");
		mappings.add("kpxx", "application/vnd.ds-keypoint");
		mappings.add("ksp", "application/vnd.kde.kspread");
		mappings.add("ktr", "application/vnd.kahootz");
		mappings.add("ktx", "image/ktx");
		mappings.add("ktz", "application/vnd.kahootz");
		mappings.add("kwd", "application/vnd.kde.kword");
		mappings.add("kwt", "application/vnd.kde.kword");
		mappings.add("lasxml", "application/vnd.las.las+xml");
		mappings.add("latex", "application/x-latex");
		mappings.add("lbd", "application/vnd.llamagraphics.life-balance.desktop");
		mappings.add("lbe", "application/vnd.llamagraphics.life-balance.exchange+xml");
		mappings.add("les", "application/vnd.hhe.lesson-player");
		mappings.add("lha", "application/x-lzh-compressed");
		mappings.add("link66", "application/vnd.route66.link66+xml");
		mappings.add("list", "text/plain");
		mappings.add("list3820", "application/vnd.ibm.modcap");
		mappings.add("listafp", "application/vnd.ibm.modcap");
		mappings.add("lnk", "application/x-ms-shortcut");
		mappings.add("log", "text/plain");
		mappings.add("lostxml", "application/lost+xml");
		mappings.add("lrf", "application/octet-stream");
		mappings.add("lrm", "application/vnd.ms-lrm");
		mappings.add("ltf", "application/vnd.frogans.ltf");
		mappings.add("lvp", "audio/vnd.lucent.voice");
		mappings.add("lwp", "application/vnd.lotus-wordpro");
		mappings.add("lzh", "application/x-lzh-compressed");
		mappings.add("m13", "application/x-msmediaview");
		mappings.add("m14", "application/x-msmediaview");
		mappings.add("m1v", "video/mpeg");
		mappings.add("m21", "application/mp21");
		mappings.add("m2a", "audio/mpeg");
		mappings.add("m2v", "video/mpeg");
		mappings.add("m3a", "audio/mpeg");
		mappings.add("m3u", "audio/x-mpegurl");
		mappings.add("m3u8", "application/vnd.apple.mpegurl");
		mappings.add("m4a", "audio/mp4");
		mappings.add("m4b", "audio/mp4");
		mappings.add("m4r", "audio/mp4");
		mappings.add("m4u", "video/vnd.mpegurl");
		mappings.add("m4v", "video/mp4");
		mappings.add("ma", "application/mathematica");
		mappings.add("mac", "image/x-macpaint");
		mappings.add("mads", "application/mads+xml");
		mappings.add("mag", "application/vnd.ecowin.chart");
		mappings.add("maker", "application/vnd.framemaker");
		mappings.add("man", "text/troff");
		mappings.add("mar", "application/octet-stream");
		mappings.add("mathml", "application/mathml+xml");
		mappings.add("mb", "application/mathematica");
		mappings.add("mbk", "application/vnd.mobius.mbk");
		mappings.add("mbox", "application/mbox");
		mappings.add("mc1", "application/vnd.medcalcdata");
		mappings.add("mcd", "application/vnd.mcd");
		mappings.add("mcurl", "text/vnd.curl.mcurl");
		mappings.add("mdb", "application/x-msaccess");
		mappings.add("mdi", "image/vnd.ms-modi");
		mappings.add("me", "text/troff");
		mappings.add("mesh", "model/mesh");
		mappings.add("meta4", "application/metalink4+xml");
		mappings.add("metalink", "application/metalink+xml");
		mappings.add("mets", "application/mets+xml");
		mappings.add("mfm", "application/vnd.mfmp");
		mappings.add("mft", "application/rpki-manifest");
		mappings.add("mgp", "application/vnd.osgeo.mapguide.package");
		mappings.add("mgz", "application/vnd.proteus.magazine");
		mappings.add("mid", "audio/midi");
		mappings.add("midi", "audio/midi");
		mappings.add("mie", "application/x-mie");
		mappings.add("mif", "application/x-mif");
		mappings.add("mime", "message/rfc822");
		mappings.add("mj2", "video/mj2");
		mappings.add("mjp2", "video/mj2");
		mappings.add("mk3d", "video/x-matroska");
		mappings.add("mka", "audio/x-matroska");
		mappings.add("mks", "video/x-matroska");
		mappings.add("mkv", "video/x-matroska");
		mappings.add("mlp", "application/vnd.dolby.mlp");
		mappings.add("mmd", "application/vnd.chipnuts.karaoke-mmd");
		mappings.add("mmf", "application/vnd.smaf");
		mappings.add("mmr", "image/vnd.fujixerox.edmics-mmr");
		mappings.add("mng", "video/x-mng");
		mappings.add("mny", "application/x-msmoney");
		mappings.add("mobi", "application/x-mobipocket-ebook");
		mappings.add("mods", "application/mods+xml");
		mappings.add("mov", "video/quicktime");
		mappings.add("movie", "video/x-sgi-movie");
		mappings.add("mp1", "audio/mpeg");
		mappings.add("mp2", "audio/mpeg");
		mappings.add("mp21", "application/mp21");
		mappings.add("mp2a", "audio/mpeg");
		mappings.add("mp3", "audio/mpeg");
		mappings.add("mp4", "video/mp4");
		mappings.add("mp4a", "audio/mp4");
		mappings.add("mp4s", "application/mp4");
		mappings.add("mp4v", "video/mp4");
		mappings.add("mpa", "audio/mpeg");
		mappings.add("mpc", "application/vnd.mophun.certificate");
		mappings.add("mpe", "video/mpeg");
		mappings.add("mpeg", "video/mpeg");
		mappings.add("mpega", "audio/x-mpeg");
		mappings.add("mpg", "video/mpeg");
		mappings.add("mpg4", "video/mp4");
		mappings.add("mpga", "audio/mpeg");
		mappings.add("mpkg", "application/vnd.apple.installer+xml");
		mappings.add("mpm", "application/vnd.blueice.multipass");
		mappings.add("mpn", "application/vnd.mophun.application");
		mappings.add("mpp", "application/vnd.ms-project");
		mappings.add("mpt", "application/vnd.ms-project");
		mappings.add("mpv2", "video/mpeg2");
		mappings.add("mpy", "application/vnd.ibm.minipay");
		mappings.add("mqy", "application/vnd.mobius.mqy");
		mappings.add("mrc", "application/marc");
		mappings.add("mrcx", "application/marcxml+xml");
		mappings.add("ms", "application/x-wais-source");
		mappings.add("mscml", "application/mediaservercontrol+xml");
		mappings.add("mseed", "application/vnd.fdsn.mseed");
		mappings.add("mseq", "application/vnd.mseq");
		mappings.add("msf", "application/vnd.epson.msf");
		mappings.add("msh", "model/mesh");
		mappings.add("msi", "application/x-msdownload");
		mappings.add("msl", "application/vnd.mobius.msl");
		mappings.add("msty", "application/vnd.muvee.style");
		mappings.add("mts", "model/vnd.mts");
		mappings.add("mus", "application/vnd.musician");
		mappings.add("musicxml", "application/vnd.recordare.musicxml+xml");
		mappings.add("mvb", "application/x-msmediaview");
		mappings.add("mwf", "application/vnd.mfer");
		mappings.add("mxf", "application/mxf");
		mappings.add("mxl", "application/vnd.recordare.musicxml");
		mappings.add("mxml", "application/xv+xml");
		mappings.add("mxs", "application/vnd.triscape.mxs");
		mappings.add("mxu", "video/vnd.mpegurl");
		mappings.add("n-gage", "application/vnd.nokia.n-gage.symbian.install");
		mappings.add("n3", "text/n3");
		mappings.add("nb", "application/mathematica");
		mappings.add("nbp", "application/vnd.wolfram.player");
		mappings.add("nc", "application/x-netcdf");
		mappings.add("ncx", "application/x-dtbncx+xml");
		mappings.add("nfo", "text/x-nfo");
		mappings.add("ngdat", "application/vnd.nokia.n-gage.data");
		mappings.add("nitf", "application/vnd.nitf");
		mappings.add("nlu", "application/vnd.neurolanguage.nlu");
		mappings.add("nml", "application/vnd.enliven");
		mappings.add("nnd", "application/vnd.noblenet-directory");
		mappings.add("nns", "application/vnd.noblenet-sealer");
		mappings.add("nnw", "application/vnd.noblenet-web");
		mappings.add("npx", "image/vnd.net-fpx");
		mappings.add("nsc", "application/x-conference");
		mappings.add("nsf", "application/vnd.lotus-notes");
		mappings.add("ntf", "application/vnd.nitf");
		mappings.add("nzb", "application/x-nzb");
		mappings.add("oa2", "application/vnd.fujitsu.oasys2");
		mappings.add("oa3", "application/vnd.fujitsu.oasys3");
		mappings.add("oas", "application/vnd.fujitsu.oasys");
		mappings.add("obd", "application/x-msbinder");
		mappings.add("obj", "application/x-tgif");
		mappings.add("oda", "application/oda");
		mappings.add("odb", "application/vnd.oasis.opendocument.database");
		mappings.add("odc", "application/vnd.oasis.opendocument.chart");
		mappings.add("odf", "application/vnd.oasis.opendocument.formula");
		mappings.add("odft", "application/vnd.oasis.opendocument.formula-template");
		mappings.add("odg", "application/vnd.oasis.opendocument.graphics");
		mappings.add("odi", "application/vnd.oasis.opendocument.image");
		mappings.add("odm", "application/vnd.oasis.opendocument.text-master");
		mappings.add("odp", "application/vnd.oasis.opendocument.presentation");
		mappings.add("ods", "application/vnd.oasis.opendocument.spreadsheet");
		mappings.add("odt", "application/vnd.oasis.opendocument.text");
		mappings.add("oga", "audio/ogg");
		mappings.add("ogg", "audio/ogg");
		mappings.add("ogv", "video/ogg");
		mappings.add("ogx", "application/ogg");
		mappings.add("omdoc", "application/omdoc+xml");
		mappings.add("onepkg", "application/onenote");
		mappings.add("onetmp", "application/onenote");
		mappings.add("onetoc", "application/onenote");
		mappings.add("onetoc2", "application/onenote");
		mappings.add("opf", "application/oebps-package+xml");
		mappings.add("opml", "text/x-opml");
		mappings.add("oprc", "application/vnd.palm");
		mappings.add("org", "application/vnd.lotus-organizer");
		mappings.add("osf", "application/vnd.yamaha.openscoreformat");
		mappings.add("osfpvg", "application/vnd.yamaha.openscoreformat.osfpvg+xml");
		mappings.add("otc", "application/vnd.oasis.opendocument.chart-template");
		mappings.add("otf", "application/x-font-opentype");
		mappings.add("otg", "application/vnd.oasis.opendocument.graphics-template");
		mappings.add("oth", "application/vnd.oasis.opendocument.text-web");
		mappings.add("oti", "application/vnd.oasis.opendocument.image-template");
		mappings.add("otp", "application/vnd.oasis.opendocument.presentation-template");
		mappings.add("ots", "application/vnd.oasis.opendocument.spreadsheet-template");
		mappings.add("ott", "application/vnd.oasis.opendocument.text-template");
		mappings.add("oxps", "application/oxps");
		mappings.add("oxt", "application/vnd.openofficeorg.extension");
		mappings.add("p", "text/x-pascal");
		mappings.add("p10", "application/pkcs10");
		mappings.add("p12", "application/x-pkcs12");
		mappings.add("p7b", "application/x-pkcs7-certificates");
		mappings.add("p7c", "application/pkcs7-mime");
		mappings.add("p7m", "application/pkcs7-mime");
		mappings.add("p7r", "application/x-pkcs7-certreqresp");
		mappings.add("p7s", "application/pkcs7-signature");
		mappings.add("p8", "application/pkcs8");
		mappings.add("pas", "text/x-pascal");
		mappings.add("paw", "application/vnd.pawaafile");
		mappings.add("pbd", "application/vnd.powerbuilder6");
		mappings.add("pbm", "image/x-portable-bitmap");
		mappings.add("pcap", "application/vnd.tcpdump.pcap");
		mappings.add("pcf", "application/x-font-pcf");
		mappings.add("pcl", "application/vnd.hp-pcl");
		mappings.add("pclxl", "application/vnd.hp-pclxl");
		mappings.add("pct", "image/pict");
		mappings.add("pcurl", "application/vnd.curl.pcurl");
		mappings.add("pcx", "image/x-pcx");
		mappings.add("pdb", "application/vnd.palm");
		mappings.add("pdf", "application/pdf");
		mappings.add("pfa", "application/x-font-type1");
		mappings.add("pfb", "application/x-font-type1");
		mappings.add("pfm", "application/x-font-type1");
		mappings.add("pfr", "application/font-tdpfr");
		mappings.add("pfx", "application/x-pkcs12");
		mappings.add("pgm", "image/x-portable-graymap");
		mappings.add("pgn", "application/x-chess-pgn");
		mappings.add("pgp", "application/pgp-encrypted");
		mappings.add("pic", "image/pict");
		mappings.add("pict", "image/pict");
		mappings.add("pkg", "application/octet-stream");
		mappings.add("pki", "application/pkixcmp");
		mappings.add("pkipath", "application/pkix-pkipath");
		mappings.add("plb", "application/vnd.3gpp.pic-bw-large");
		mappings.add("plc", "application/vnd.mobius.plc");
		mappings.add("plf", "application/vnd.pocketlearn");
		mappings.add("pls", "audio/x-scpls");
		mappings.add("pml", "application/vnd.ctc-posml");
		mappings.add("png", "image/png");
		mappings.add("pnm", "image/x-portable-anymap");
		mappings.add("pnt", "image/x-macpaint");
		mappings.add("portpkg", "application/vnd.macports.portpkg");
		mappings.add("pot", "application/vnd.ms-powerpoint");
		mappings.add("potm", "application/vnd.ms-powerpoint.template.macroenabled.12");
		mappings.add("potx", "application/vnd.openxmlformats-officedocument.presentationml.template");
		mappings.add("ppam", "application/vnd.ms-powerpoint.addin.macroenabled.12");
		mappings.add("ppd", "application/vnd.cups-ppd");
		mappings.add("ppm", "image/x-portable-pixmap");
		mappings.add("pps", "application/vnd.ms-powerpoint");
		mappings.add("ppsm", "application/vnd.ms-powerpoint.slideshow.macroenabled.12");
		mappings.add("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
		mappings.add("ppt", "application/vnd.ms-powerpoint");
		mappings.add("pptm", "application/vnd.ms-powerpoint.presentation.macroenabled.12");
		mappings.add("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		mappings.add("pqa", "application/vnd.palm");
		mappings.add("prc", "application/x-mobipocket-ebook");
		mappings.add("pre", "application/vnd.lotus-freelance");
		mappings.add("prf", "application/pics-rules");
		mappings.add("ps", "application/postscript");
		mappings.add("psb", "application/vnd.3gpp.pic-bw-small");
		mappings.add("psd", "image/vnd.adobe.photoshop");
		mappings.add("psf", "application/x-font-linux-psf");
		mappings.add("pskcxml", "application/pskc+xml");
		mappings.add("ptid", "application/vnd.pvi.ptid1");
		mappings.add("pub", "application/x-mspublisher");
		mappings.add("pvb", "application/vnd.3gpp.pic-bw-var");
		mappings.add("pwn", "application/vnd.3m.post-it-notes");
		mappings.add("pya", "audio/vnd.ms-playready.media.pya");
		mappings.add("pyv", "video/vnd.ms-playready.media.pyv");
		mappings.add("qam", "application/vnd.epson.quickanime");
		mappings.add("qbo", "application/vnd.intu.qbo");
		mappings.add("qfx", "application/vnd.intu.qfx");
		mappings.add("qps", "application/vnd.publishare-delta-tree");
		mappings.add("qt", "video/quicktime");
		mappings.add("qti", "image/x-quicktime");
		mappings.add("qtif", "image/x-quicktime");
		mappings.add("qwd", "application/vnd.quark.quarkxpress");
		mappings.add("qwt", "application/vnd.quark.quarkxpress");
		mappings.add("qxb", "application/vnd.quark.quarkxpress");
		mappings.add("qxd", "application/vnd.quark.quarkxpress");
		mappings.add("qxl", "application/vnd.quark.quarkxpress");
		mappings.add("qxt", "application/vnd.quark.quarkxpress");
		mappings.add("ra", "audio/x-pn-realaudio");
		mappings.add("ram", "audio/x-pn-realaudio");
		mappings.add("rar", "application/x-rar-compressed");
		mappings.add("ras", "image/x-cmu-raster");
		mappings.add("rcprofile", "application/vnd.ipunplugged.rcprofile");
		mappings.add("rdf", "application/rdf+xml");
		mappings.add("rdz", "application/vnd.data-vision.rdz");
		mappings.add("rep", "application/vnd.businessobjects");
		mappings.add("res", "application/x-dtbresource+xml");
		mappings.add("rgb", "image/x-rgb");
		mappings.add("rif", "application/reginfo+xml");
		mappings.add("rip", "audio/vnd.rip");
		mappings.add("ris", "application/x-research-info-systems");
		mappings.add("rl", "application/resource-lists+xml");
		mappings.add("rlc", "image/vnd.fujixerox.edmics-rlc");
		mappings.add("rld", "application/resource-lists-diff+xml");
		mappings.add("rm", "application/vnd.rn-realmedia");
		mappings.add("rmi", "audio/midi");
		mappings.add("rmp", "audio/x-pn-realaudio-plugin");
		mappings.add("rms", "application/vnd.jcp.javame.midlet-rms");
		mappings.add("rmvb", "application/vnd.rn-realmedia-vbr");
		mappings.add("rnc", "application/relax-ng-compact-syntax");
		mappings.add("roa", "application/rpki-roa");
		mappings.add("roff", "text/troff");
		mappings.add("rp9", "application/vnd.cloanto.rp9");
		mappings.add("rpss", "application/vnd.nokia.radio-presets");
		mappings.add("rpst", "application/vnd.nokia.radio-preset");
		mappings.add("rq", "application/sparql-query");
		mappings.add("rs", "application/rls-services+xml");
		mappings.add("rsd", "application/rsd+xml");
		mappings.add("rss", "application/rss+xml");
		mappings.add("rtf", "application/rtf");
		mappings.add("rtx", "text/richtext");
		mappings.add("s", "text/x-asm");
		mappings.add("s3m", "audio/s3m");
		mappings.add("saf", "application/vnd.yamaha.smaf-audio");
		mappings.add("sbml", "application/sbml+xml");
		mappings.add("sc", "application/vnd.ibm.secure-container");
		mappings.add("scd", "application/x-msschedule");
		mappings.add("scm", "application/vnd.lotus-screencam");
		mappings.add("scq", "application/scvp-cv-request");
		mappings.add("scs", "application/scvp-cv-response");
		mappings.add("scurl", "text/vnd.curl.scurl");
		mappings.add("sda", "application/vnd.stardivision.draw");
		mappings.add("sdc", "application/vnd.stardivision.calc");
		mappings.add("sdd", "application/vnd.stardivision.impress");
		mappings.add("sdkd", "application/vnd.solent.sdkm+xml");
		mappings.add("sdkm", "application/vnd.solent.sdkm+xml");
		mappings.add("sdp", "application/sdp");
		mappings.add("sdw", "application/vnd.stardivision.writer");
		mappings.add("see", "application/vnd.seemail");
		mappings.add("seed", "application/vnd.fdsn.seed");
		mappings.add("sema", "application/vnd.sema");
		mappings.add("semd", "application/vnd.semd");
		mappings.add("semf", "application/vnd.semf");
		mappings.add("ser", "application/java-serialized-object");
		mappings.add("setpay", "application/set-payment-initiation");
		mappings.add("setreg", "application/set-registration-initiation");
		mappings.add("sfd-hdstx", "application/vnd.hydrostatix.sof-data");
		mappings.add("sfnt", "application/font-sfnt");
		mappings.add("sfs", "application/vnd.spotfire.sfs");
		mappings.add("sfv", "text/x-sfv");
		mappings.add("sgi", "image/sgi");
		mappings.add("sgl", "application/vnd.stardivision.writer-global");
		mappings.add("sgm", "text/sgml");
		mappings.add("sgml", "text/sgml");
		mappings.add("sh", "application/x-sh");
		mappings.add("shar", "application/x-shar");
		mappings.add("shf", "application/shf+xml");
		mappings.add("sid", "image/x-mrsid-image");
		mappings.add("sig", "application/pgp-signature");
		mappings.add("sil", "audio/silk");
		mappings.add("silo", "model/mesh");
		mappings.add("sis", "application/vnd.symbian.install");
		mappings.add("sisx", "application/vnd.symbian.install");
		mappings.add("sit", "application/x-stuffit");
		mappings.add("sitx", "application/x-stuffitx");
		mappings.add("skd", "application/vnd.koan");
		mappings.add("skm", "application/vnd.koan");
		mappings.add("skp", "application/vnd.koan");
		mappings.add("skt", "application/vnd.koan");
		mappings.add("sldm", "application/vnd.ms-powerpoint.slide.macroenabled.12");
		mappings.add("sldx", "application/vnd.openxmlformats-officedocument.presentationml.slide");
		mappings.add("slt", "application/vnd.epson.salt");
		mappings.add("sm", "application/vnd.stepmania.stepchart");
		mappings.add("smf", "application/vnd.stardivision.math");
		mappings.add("smi", "application/smil+xml");
		mappings.add("smil", "application/smil+xml");
		mappings.add("smv", "video/x-smv");
		mappings.add("smzip", "application/vnd.stepmania.package");
		mappings.add("snd", "audio/basic");
		mappings.add("snf", "application/x-font-snf");
		mappings.add("so", "application/octet-stream");
		mappings.add("spc", "application/x-pkcs7-certificates");
		mappings.add("spf", "application/vnd.yamaha.smaf-phrase");
		mappings.add("spl", "application/x-futuresplash");
		mappings.add("spot", "text/vnd.in3d.spot");
		mappings.add("spp", "application/scvp-vp-response");
		mappings.add("spq", "application/scvp-vp-request");
		mappings.add("spx", "audio/ogg");
		mappings.add("sql", "application/x-sql");
		mappings.add("src", "application/x-wais-source");
		mappings.add("srt", "application/x-subrip");
		mappings.add("sru", "application/sru+xml");
		mappings.add("srx", "application/sparql-results+xml");
		mappings.add("ssdl", "application/ssdl+xml");
		mappings.add("sse", "application/vnd.kodak-descriptor");
		mappings.add("ssf", "application/vnd.epson.ssf");
		mappings.add("ssml", "application/ssml+xml");
		mappings.add("st", "application/vnd.sailingtracker.track");
		mappings.add("stc", "application/vnd.sun.xml.calc.template");
		mappings.add("std", "application/vnd.sun.xml.draw.template");
		mappings.add("stf", "application/vnd.wt.stf");
		mappings.add("sti", "application/vnd.sun.xml.impress.template");
		mappings.add("stk", "application/hyperstudio");
		mappings.add("stl", "application/vnd.ms-pki.stl");
		mappings.add("str", "application/vnd.pg.format");
		mappings.add("stw", "application/vnd.sun.xml.writer.template");
		mappings.add("sub", "text/vnd.dvb.subtitle");
		mappings.add("sus", "application/vnd.sus-calendar");
		mappings.add("susp", "application/vnd.sus-calendar");
		mappings.add("sv4cpio", "application/x-sv4cpio");
		mappings.add("sv4crc", "application/x-sv4crc");
		mappings.add("svc", "application/vnd.dvb.service");
		mappings.add("svd", "application/vnd.svd");
		mappings.add("svg", "image/svg+xml");
		mappings.add("svgz", "image/svg+xml");
		mappings.add("swa", "application/x-director");
		mappings.add("swf", "application/x-shockwave-flash");
		mappings.add("swi", "application/vnd.aristanetworks.swi");
		mappings.add("sxc", "application/vnd.sun.xml.calc");
		mappings.add("sxd", "application/vnd.sun.xml.draw");
		mappings.add("sxg", "application/vnd.sun.xml.writer.global");
		mappings.add("sxi", "application/vnd.sun.xml.impress");
		mappings.add("sxm", "application/vnd.sun.xml.math");
		mappings.add("sxw", "application/vnd.sun.xml.writer");
		mappings.add("t", "text/troff");
		mappings.add("t3", "application/x-t3vm-image");
		mappings.add("taglet", "application/vnd.mynfc");
		mappings.add("tao", "application/vnd.tao.intent-module-archive");
		mappings.add("tar", "application/x-tar");
		mappings.add("tcap", "application/vnd.3gpp2.tcap");
		mappings.add("tcl", "application/x-tcl");
		mappings.add("teacher", "application/vnd.smart.teacher");
		mappings.add("tei", "application/tei+xml");
		mappings.add("teicorpus", "application/tei+xml");
		mappings.add("tex", "application/x-tex");
		mappings.add("texi", "application/x-texinfo");
		mappings.add("texinfo", "application/x-texinfo");
		mappings.add("text", "text/plain");
		mappings.add("tfi", "application/thraud+xml");
		mappings.add("tfm", "application/x-tex-tfm");
		mappings.add("tga", "image/x-tga");
		mappings.add("thmx", "application/vnd.ms-officetheme");
		mappings.add("tif", "image/tiff");
		mappings.add("tiff", "image/tiff");
		mappings.add("tmo", "application/vnd.tmobile-livetv");
		mappings.add("torrent", "application/x-bittorrent");
		mappings.add("tpl", "application/vnd.groove-tool-template");
		mappings.add("tpt", "application/vnd.trid.tpt");
		mappings.add("tr", "text/troff");
		mappings.add("tra", "application/vnd.trueapp");
		mappings.add("trm", "application/x-msterminal");
		mappings.add("tsd", "application/timestamped-data");
		mappings.add("tsv", "text/tab-separated-values");
		mappings.add("ttc", "font/collection");
		mappings.add("ttf", "application/x-font-ttf");
		mappings.add("ttl", "text/turtle");
		mappings.add("twd", "application/vnd.simtech-mindmapper");
		mappings.add("twds", "application/vnd.simtech-mindmapper");
		mappings.add("txd", "application/vnd.genomatix.tuxedo");
		mappings.add("txf", "application/vnd.mobius.txf");
		mappings.add("txt", "text/plain");
		mappings.add("u32", "application/x-authorware-bin");
		mappings.add("udeb", "application/x-debian-package");
		mappings.add("ufd", "application/vnd.ufdl");
		mappings.add("ufdl", "application/vnd.ufdl");
		mappings.add("ulw", "audio/basic");
		mappings.add("ulx", "application/x-glulx");
		mappings.add("umj", "application/vnd.umajin");
		mappings.add("unityweb", "application/vnd.unity");
		mappings.add("uoml", "application/vnd.uoml+xml");
		mappings.add("uri", "text/uri-list");
		mappings.add("uris", "text/uri-list");
		mappings.add("urls", "text/uri-list");
		mappings.add("ustar", "application/x-ustar");
		mappings.add("utz", "application/vnd.uiq.theme");
		mappings.add("uu", "text/x-uuencode");
		mappings.add("uva", "audio/vnd.dece.audio");
		mappings.add("uvd", "application/vnd.dece.data");
		mappings.add("uvf", "application/vnd.dece.data");
		mappings.add("uvg", "image/vnd.dece.graphic");
		mappings.add("uvh", "video/vnd.dece.hd");
		mappings.add("uvi", "image/vnd.dece.graphic");
		mappings.add("uvm", "video/vnd.dece.mobile");
		mappings.add("uvp", "video/vnd.dece.pd");
		mappings.add("uvs", "video/vnd.dece.sd");
		mappings.add("uvt", "application/vnd.dece.ttml+xml");
		mappings.add("uvu", "video/vnd.uvvu.mp4");
		mappings.add("uvv", "video/vnd.dece.video");
		mappings.add("uvva", "audio/vnd.dece.audio");
		mappings.add("uvvd", "application/vnd.dece.data");
		mappings.add("uvvf", "application/vnd.dece.data");
		mappings.add("uvvg", "image/vnd.dece.graphic");
		mappings.add("uvvh", "video/vnd.dece.hd");
		mappings.add("uvvi", "image/vnd.dece.graphic");
		mappings.add("uvvm", "video/vnd.dece.mobile");
		mappings.add("uvvp", "video/vnd.dece.pd");
		mappings.add("uvvs", "video/vnd.dece.sd");
		mappings.add("uvvt", "application/vnd.dece.ttml+xml");
		mappings.add("uvvu", "video/vnd.uvvu.mp4");
		mappings.add("uvvv", "video/vnd.dece.video");
		mappings.add("uvvx", "application/vnd.dece.unspecified");
		mappings.add("uvvz", "application/vnd.dece.zip");
		mappings.add("uvx", "application/vnd.dece.unspecified");
		mappings.add("uvz", "application/vnd.dece.zip");
		mappings.add("vcard", "text/vcard");
		mappings.add("vcd", "application/x-cdlink");
		mappings.add("vcf", "text/x-vcard");
		mappings.add("vcg", "application/vnd.groove-vcard");
		mappings.add("vcs", "text/x-vcalendar");
		mappings.add("vcx", "application/vnd.vcx");
		mappings.add("vis", "application/vnd.visionary");
		mappings.add("viv", "video/vnd.vivo");
		mappings.add("vob", "video/x-ms-vob");
		mappings.add("vor", "application/vnd.stardivision.writer");
		mappings.add("vox", "application/x-authorware-bin");
		mappings.add("vrml", "model/vrml");
		mappings.add("vsd", "application/vnd.visio");
		mappings.add("vsf", "application/vnd.vsf");
		mappings.add("vss", "application/vnd.visio");
		mappings.add("vst", "application/vnd.visio");
		mappings.add("vsw", "application/vnd.visio");
		mappings.add("vtu", "model/vnd.vtu");
		mappings.add("vxml", "application/voicexml+xml");
		mappings.add("w3d", "application/x-director");
		mappings.add("wad", "application/x-doom");
		mappings.add("wasm", "application/wasm");
		mappings.add("wav", "audio/x-wav");
		mappings.add("wax", "audio/x-ms-wax");
		mappings.add("wbmp", "image/vnd.wap.wbmp");
		mappings.add("wbs", "application/vnd.criticaltools.wbs+xml");
		mappings.add("wbxml", "application/vnd.wap.wbxml");
		mappings.add("wcm", "application/vnd.ms-works");
		mappings.add("wdb", "application/vnd.ms-works");
		mappings.add("wdp", "image/vnd.ms-photo");
		mappings.add("weba", "audio/webm");
		mappings.add("webm", "video/webm");
		mappings.add("webp", "image/webp");
		mappings.add("wg", "application/vnd.pmi.widget");
		mappings.add("wgt", "application/widget");
		mappings.add("wks", "application/vnd.ms-works");
		mappings.add("wm", "video/x-ms-wm");
		mappings.add("wma", "audio/x-ms-wma");
		mappings.add("wmd", "application/x-ms-wmd");
		mappings.add("wmf", "application/x-msmetafile");
		mappings.add("wml", "text/vnd.wap.wml");
		mappings.add("wmlc", "application/vnd.wap.wmlc");
		mappings.add("wmls", "text/vnd.wap.wmlsc");
		mappings.add("wmlsc", "application/vnd.wap.wmlscriptc");
		mappings.add("wmlscriptc", "application/vnd.wap.wmlscriptc");
		mappings.add("wmv", "video/x-ms-wmv");
		mappings.add("wmx", "video/x-ms-wmx");
		mappings.add("wmz", "application/x-msmetafile");
		mappings.add("woff", "application/font-woff");
		mappings.add("woff2", "application/font-woff2");
		mappings.add("wpd", "application/vnd.wordperfect");
		mappings.add("wpl", "application/vnd.ms-wpl");
		mappings.add("wps", "application/vnd.ms-works");
		mappings.add("wqd", "application/vnd.wqd");
		mappings.add("wri", "application/x-mswrite");
		mappings.add("wrl", "model/vrml");
		mappings.add("wsdl", "application/wsdl+xml");
		mappings.add("wspolicy", "application/wspolicy+xml");
		mappings.add("wtb", "application/vnd.webturbo");
		mappings.add("wvx", "video/x-ms-wvx");
		mappings.add("x32", "application/x-authorware-bin");
		mappings.add("x3d", "model/x3d+xml");
		mappings.add("x3db", "model/x3d+binary");
		mappings.add("x3dbz", "model/x3d+binary");
		mappings.add("x3dv", "model/x3d+vrml");
		mappings.add("x3dvz", "model/x3d+vrml");
		mappings.add("x3dz", "model/x3d+xml");
		mappings.add("xaml", "application/xaml+xml");
		mappings.add("xap", "application/x-silverlight-app");
		mappings.add("xar", "application/vnd.xara");
		mappings.add("xbap", "application/x-ms-xbap");
		mappings.add("xbd", "application/vnd.fujixerox.docuworks.binder");
		mappings.add("xbm", "image/x-xbitmap");
		mappings.add("xdf", "application/xcap-diff+xml");
		mappings.add("xdm", "application/vnd.syncml.dm+xml");
		mappings.add("xdp", "application/vnd.adobe.xdp+xml");
		mappings.add("xdssc", "application/dssc+xml");
		mappings.add("xdw", "application/vnd.fujixerox.docuworks");
		mappings.add("xenc", "application/xenc+xml");
		mappings.add("xer", "application/patch-ops-error+xml");
		mappings.add("xfdf", "application/vnd.adobe.xfdf");
		mappings.add("xfdl", "application/vnd.xfdl");
		mappings.add("xht", "application/xhtml+xml");
		mappings.add("xhtml", "application/xhtml+xml");
		mappings.add("xhvml", "application/xv+xml");
		mappings.add("xif", "image/vnd.xiff");
		mappings.add("xla", "application/vnd.ms-excel");
		mappings.add("xlam", "application/vnd.ms-excel.addin.macroenabled.12");
		mappings.add("xlc", "application/vnd.ms-excel");
		mappings.add("xlf", "application/x-xliff+xml");
		mappings.add("xlm", "application/vnd.ms-excel");
		mappings.add("xls", "application/vnd.ms-excel");
		mappings.add("xlsb", "application/vnd.ms-excel.sheet.binary.macroenabled.12");
		mappings.add("xlsm", "application/vnd.ms-excel.sheet.macroenabled.12");
		mappings.add("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		mappings.add("xlt", "application/vnd.ms-excel");
		mappings.add("xltm", "application/vnd.ms-excel.template.macroenabled.12");
		mappings.add("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
		mappings.add("xlw", "application/vnd.ms-excel");
		mappings.add("xm", "audio/xm");
		mappings.add("xml", "application/xml");
		mappings.add("xo", "application/vnd.olpc-sugar");
		mappings.add("xop", "application/xop+xml");
		mappings.add("xpi", "application/x-xpinstall");
		mappings.add("xpl", "application/xproc+xml");
		mappings.add("xpm", "image/x-xpixmap");
		mappings.add("xpr", "application/vnd.is-xpr");
		mappings.add("xps", "application/vnd.ms-xpsdocument");
		mappings.add("xpw", "application/vnd.intercon.formnet");
		mappings.add("xpx", "application/vnd.intercon.formnet");
		mappings.add("xsl", "application/xml");
		mappings.add("xslt", "application/xslt+xml");
		mappings.add("xsm", "application/vnd.syncml+xml");
		mappings.add("xspf", "application/xspf+xml");
		mappings.add("xul", "application/vnd.mozilla.xul+xml");
		mappings.add("xvm", "application/xv+xml");
		mappings.add("xvml", "application/xv+xml");
		mappings.add("xwd", "image/x-xwindowdump");
		mappings.add("xyz", "chemical/x-xyz");
		mappings.add("xz", "application/x-xz");
		mappings.add("yang", "application/yang");
		mappings.add("yin", "application/yin+xml");
		mappings.add("z", "application/x-compress");
		mappings.add("z1", "application/x-zmachine");
		mappings.add("z2", "application/x-zmachine");
		mappings.add("z3", "application/x-zmachine");
		mappings.add("z4", "application/x-zmachine");
		mappings.add("z5", "application/x-zmachine");
		mappings.add("z6", "application/x-zmachine");
		mappings.add("z7", "application/x-zmachine");
		mappings.add("z8", "application/x-zmachine");
		mappings.add("zaz", "application/vnd.zzazz.deck+xml");
		mappings.add("zip", "application/zip");
		mappings.add("zir", "application/vnd.zul");
		mappings.add("zirz", "application/vnd.zul");
		mappings.add("zmm", "application/vnd.handheld-entertainment+xml");
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
		this.map = (mutable ? new LinkedHashMap<>(mappings.map) : Collections.unmodifiableMap(mappings.map));
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
				return this.extension.equals(other.extension) && this.mimeType.equals(other.mimeType);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.extension.hashCode();
		}

		@Override
		public String toString() {
			return "Mapping [extension=" + this.extension + ", mimeType=" + this.mimeType + "]";
		}

	}

}
