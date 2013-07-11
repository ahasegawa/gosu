/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.build;

import gw.fs.IDirectory;
import gw.fs.IDirectoryUtil;
import gw.fs.IFile;
import gw.fs.IResource;
import gw.fs.jar.JarFileDirectoryImpl;
import gw.lang.Gosu;
import gw.lang.GosuVersion;
import gw.test.util.ITCaseUtils;
import gw.util.DynamicArray;
import gw.util.StreamUtil;
import org.fest.assertions.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Manifest;

/**
 * @author Brian Chang
 */
public class JarContentITCase extends Assert {

  private static final String GOSU_TYPELOADERS_ATTR_NAME = "Gosu-Typeloaders";
  private static final String XML_TYPELOADER = "gw.internal.xml.xsd.typeprovider.XmlSchemaResourceTypeLoader";
  private static final String WSDL_TYPELOADER = "gw.internal.xml.ws.typeprovider.WsdlTypeLoader";
  private static File _gosuCoreApiSourcesJar;
  private static DistAssemblyUtil _assembly;

  @BeforeClass
  public static void beforeTestClass() throws Exception {
    _assembly = DistAssemblyUtil.getInstance();
    _gosuCoreApiSourcesJar = new File(_assembly.getPom().getParentFile().getParent(), "gosu-core-api/target/gosu-core-api-" + _assembly.getGosuVersion() + "-sources.jar");
  }

  @Test
  public void testGosuCoreApiJar() {
    IDirectory dir = getGosuCoreApiJar();
    Assertions.assertThat(toNamesSorted(dir.listDirs())).containsExactly("META-INF", "gw");
    assertGosuCoreApiShades(dir, true);
    assertGosuCoreApiFiles(dir, true);
    Manifest mf = readManifest(dir);
    assertManifestImplementationEntries(mf);
    assertManifestContainsSourcesEntry(dir, mf, "gs,gsx");
  }

  private IDirectory getGosuCoreApiJar() {
    return getJar("gosu-core-api");
  }

  @Test
  public void testGosuCoreApiSourcesJar() {
    IDirectory dir = new JarFileDirectoryImpl(_gosuCoreApiSourcesJar);
    assertTrue(dir.file("gw/lang/reflect/TypeSystem.java").exists());
    assertTrue(dir.file("gw/lang/enhancements/CoreStringEnhancement.gsx").exists());
    assertTrue(dir.file("gw/lang/IDisposable.gs").exists());
  }

  @Test
  public void testGosuCoreJar() {
    IDirectory dir = getGosuCoreJar();
    Assertions.assertThat(toNamesSorted(dir.listDirs())).containsExactly("META-INF", "OSGI-INF", "gw");
    assertGosuCoreApiShades(dir, false);
    assertGosuCoreShades(dir, true);
    assertGosuCoreApiFiles(dir, false);
    assertGosuCoreFiles(dir, true);
    Manifest mf = readManifest(dir);
    assertManifestImplementationEntries(mf);
    assertManifestContainsSourcesEntry(dir, mf, null);
  }

  private IDirectory getGosuCoreJar() {
    return getJar("gosu-core");
  }

  @Test
  public void testGosuXmlJar() {
    IDirectory dir = getGosuXmlJar();
    Assertions.assertThat(toNamesSorted(dir.listDirs())).containsExactly("META-INF", "gw", "xml");
    assertGosuCoreApiShades(dir, false);
    assertGosuCoreShades(dir, false);
    assertGosuXmlShades(dir, true);
    assertGosuCoreApiFiles(dir, false);
    assertGosuCoreFiles(dir, false);
    assertGosuXmlFiles(dir, true);
    Manifest mf = readManifest(dir);
    assertManifestImplementationEntries(mf);
    assertEquals(XML_TYPELOADER, mf.getMainAttributes().getValue(GOSU_TYPELOADERS_ATTR_NAME));
    assertManifestContainsSourcesEntry(dir, mf, "gs,xsd");
  }

  private IDirectory getGosuXmlJar() {
    return getJar("gosu-xml");
  }

  @Test
  public void testGosuWebservicesJar() {
    IDirectory dir = getGosuWebservicesJar();
    Assertions.assertThat(toNamesSorted(dir.listDirs())).containsExactly("META-INF", "dftree", "gw", "xml");
    assertGosuCoreApiShades(dir, false);
    assertGosuCoreShades(dir, false);
    assertGosuXmlShades(dir, false);
    assertGosuWebservicesShades(dir, true);
    assertGosuCoreApiFiles(dir, false);
    assertGosuCoreFiles(dir, false);
    assertGosuXmlFiles(dir, false);
    assertGosuWebservicesFiles(dir, true);
    Manifest mf = readManifest(dir);
    assertManifestImplementationEntries(mf);
    assertEquals(WSDL_TYPELOADER, mf.getMainAttributes().getValue(GOSU_TYPELOADERS_ATTR_NAME));
    assertManifestContainsSourcesEntry(dir, mf, "gs,gsx,xsd");
  }

  private IDirectory getGosuWebservicesJar() {
    return getJar("gosu-webservices");
  }

  @Test
  public void testNoOverlapsAmongShadedJars() {
    IDirectory[] shadedJars = new IDirectory[] {
            getGosuCoreApiJar(),
            getGosuCoreJar(),
            getGosuXmlJar(),
            getGosuWebservicesJar()
    };
    TreeMap<String, List<String>> collectedResources = new TreeMap<String, List<String>>();
    for (IDirectory shadedJar : shadedJars) {
      collectResources(shadedJar, shadedJar, collectedResources);
    }
    boolean fail = false;
    for (Map.Entry<String, List<String>> resourceEntry : collectedResources.entrySet()) {
      if (resourceEntry.getValue().size() != 1
              && !resourceEntry.getKey().startsWith("META-INF/")
              && !resourceEntry.getKey().equals("internal/xml/xsd-codegen.xml")) {
        System.out.println(resourceEntry);
        fail = true;
      }
    }
    if (fail) {
      fail("shaded jars have unapproved overlapping resources - see log above for details");
    }
  }

  private void collectResources(IDirectory root, IDirectory dir, TreeMap<String, List<String>> collectedResources) {
    for (IFile file : dir.listFiles()) {
      String resourceName = IDirectoryUtil.relativePath(root, file);
      List<String> jarList = collectedResources.get(resourceName);
      if (jarList == null) {
        jarList = new ArrayList<String>(1);
        collectedResources.put(resourceName, jarList);
      }
      jarList.add(root.getName());
    }
    for (IDirectory subDir : dir.listDirs()) {
      collectResources(root, subDir, collectedResources);
    }
  }

  private void assertGosuCoreApiFiles(IDirectory dir, boolean expected) {
    assertEquals(expected, dir.file(Gosu.class.getName().replace(".", "/") + ".class").exists());
    assertEquals(expected, dir.file("gw/util/OSType.gs").exists());
  }

  private void assertGosuCoreFiles(IDirectory dir, boolean expected) {
    assertEquals(expected, dir.file("gw/internal/gosu/module/Module.class").exists());
  }

  private void assertGosuXmlFiles(IDirectory dir, boolean expected) {
    assertEquals(expected, dir.file("gw/xml/XmlElement.class").exists());
    assertEquals(expected, dir.file("gw/xml/xsd/types/XSDDateTime.gs").exists());
    assertEquals(expected, dir.file("xml/schemalocations.xml").exists());
    assertEquals(expected, dir.file(XML_TYPELOADER.replace(".", "/") + ".class").exists());
  }

  private void assertGosuWebservicesFiles(IDirectory dir, boolean expected) {
    assertEquals(expected, dir.file("gw/xml/ws/Wsdl2Gosu.class").exists());
    assertEquals(expected, dir.file("gw/xml/ws/WsdlConfig.gs").exists());
    assertEquals(expected, dir.file(WSDL_TYPELOADER.replace(".", "/") + ".class").exists());
  }

  private void assertGosuCoreApiShades(IDirectory dir, boolean expected) {
    assertFalse(dir.dir("gw/lang/launch").exists());
    assertEquals(expected, dir.dir("gw/internal/ext/org/apache/commons/cli").exists());
  }

  private void assertGosuCoreShades(IDirectory dir, boolean expected) {
    assertEquals(expected, dir.dir("gw/internal/ext/org/antlr").exists());
    assertEquals(expected, dir.dir("gw/internal/ext/org/objectweb/asm").exists());
  }

  private void assertGosuXmlShades(IDirectory dir, boolean expected) {
    assertEquals(expected, dir.dir("gw/internal/ext/org/apache/commons/collections").exists());
    assertEquals(expected, dir.dir("gw/internal/ext/org/apache/xerces").exists());
  }

  private void assertGosuWebservicesShades(IDirectory dir, boolean expected) {
    assertEquals(expected, dir.dir("gw/internal/ext/org/mortbay").exists());
    assertEquals(expected, dir.dir("gw/internal/ext/org/apache/commons/logging").exists());
  }

  private Manifest readManifest(IDirectory dir) {
    InputStream in = null;
    try {
      in = dir.file("META-INF/MANIFEST.MF").openInputStream();
      return new Manifest(in);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      try {
        StreamUtil.close(in);
      } catch (IOException e) {
        // ignore
      }
    }
  }

  private void assertManifestImplementationEntries(Manifest mf) {
    assertTrue(mf.getMainAttributes().getValue("Bundle-SymbolicName").startsWith("org.gosu-lang.gosu."));
    assertEquals(getOsgiVersion(), mf.getMainAttributes().getValue("Bundle-Version"));
  }

  private static void assertManifestContainsSourcesEntry(IDirectory dir, Manifest mf, String expectedSources) {
    HashSet<String> found = new HashSet<String>();
    DynamicArray<? extends IFile> files = IDirectoryUtil.allContainedFilesExcludingIgnored(dir);
    for (IFile file : files) {
      String extension = file.getExtension();
      if (extension.equals("gs") || extension.equals("gsx") || extension.equals("xsd")) {
        found.add(extension);
      }
    }
    List<String> foundExtensions = new ArrayList<String>(found);
    Collections.sort(foundExtensions);

    if (expectedSources != null) {
      List<String> expectedSourceExtensions = Arrays.asList(expectedSources.split(","));
      Assertions.assertThat(foundExtensions)
              .as("the set of extensions in the manifest (Contains-Sources) don't match the set found in the jar")
              .isEqualTo(expectedSourceExtensions);

      assertEquals(expectedSources, mf.getMainAttributes().getValue("Contains-Sources"));
    }
    else {
      Assertions.assertThat(foundExtensions).isEmpty();
      assertNull(mf.getMainAttributes().getValue("Contains-Sources"));
    }
  }

  private static String getOsgiVersion() {
    GosuVersion version = GosuVersion.parse(_assembly.getGosuVersion());
    String osgiVersion = version.getMajor() + "." + version.getMinor() + "." + version.getIncremental();
    if (version.getBuildNum() > 0) {
      osgiVersion += "." + version.getBuildNum();
    }
    else if (version.getQualifier() != null) {
      osgiVersion += "." + version.getQualifier();
    }
    return osgiVersion;
  }

  private IDirectory getJar(String name) {
    File jar = _assembly.getJar(name);
    return new JarFileDirectoryImpl(jar);
  }

  private List<String> toNamesSorted(List<? extends IResource> dirs) {
    return ITCaseUtils.toNamesSorted(dirs);
  }

}
