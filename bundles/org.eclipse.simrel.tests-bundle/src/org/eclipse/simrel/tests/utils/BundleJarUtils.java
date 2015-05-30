/**
 * 
 */
package org.eclipse.simrel.tests.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

/**
 * @author dhuebner
 *
 */
public class BundleJarUtils {
    /**
     * @param fileToUnpack
     *            jar.pack.gz file
     * @return unpacked jar file stored in the temp folder. This file is marked
     *         as delete on VM exit.
     * @throws IOException
     */
    public static File unpack200gz(File fileToUnpack) throws IOException {
        GZIPInputStream input = new GZIPInputStream(new FileInputStream(fileToUnpack));
        File unpacked = File.createTempFile("unpacked_" + fileToUnpack.getName() + "_", ".jar");
        unpacked.deleteOnExit();
        FileOutputStream outStream = new FileOutputStream(unpacked);
        JarOutputStream jarOutStream = new JarOutputStream(outStream);
        Pack200.Unpacker unpacker = Pack200.newUnpacker();
        unpacker.unpack(input, jarOutStream);
        jarOutStream.close();
        return unpacked;
    }

    /**
     * Return the bundle id from the manifest pointed to by the given input
     * stream.
     */
    public static String getJarManifestEntry(InputStream input, String key) {
        String manifestEntry = null;
        try {
            Map attributes = ManifestElement.parseBundleManifest(input, null);
            manifestEntry = (String) attributes.get(key);
        } catch (BundleException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return manifestEntry;
    }

    /**
     * The given file points to a bundle contained in an archive. Look into the
     * bundle manifest file to find the bundle identifier.
     */
    public static String getJarManifestEntry(File file, String key) {
        InputStream input = null;
        JarFile jar = null;
        try {
            jar = new JarFile(file, false, ZipFile.OPEN_READ);
            JarEntry entry = jar.getJarEntry(JarFile.MANIFEST_NAME);
            if (entry == null) {
                // addError("Bundle does not contain a MANIFEST.MF file: " +
                // file.getAbsolutePath());
                return null;
            }
            input = jar.getInputStream(entry);
            return getJarManifestEntry(input, key);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            // addError(e.getMessage());
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static Properties getEclipseInf(File jarfile) {
        JarFile jar = null;
        Properties properties = new Properties();
        try {
            jar = new JarFile(jarfile, false, ZipFile.OPEN_READ);
            JarEntry eclipseInf = jar.getJarEntry("META-INF/eclipse.inf");
            if (eclipseInf != null) {
                properties.load(jar.getInputStream(eclipseInf));
            }
        } catch (ZipException e) {
            System.out.println("Failed to open jar file (zip exception): " + jarfile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return properties;
    }
}