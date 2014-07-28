/**
 * 
 */
package org.eclipse.simrel.tests.common.checker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.utils.IUUtil;
import org.osgi.framework.Constants;

/**
 * @author dhuebner
 *
 */
public class BREEChecker implements IArtifactChecker {

	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu,
			File child) {
		CheckReport report = new CheckReport(BREEChecker.class, iu);
		try {
			@SuppressWarnings("deprecation")
			String bree = IUUtil.getBundleManifestEntry(child, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
			boolean needsBree = needsBree(child);
			if ((bree != null) && (bree.length() > 0)) {
				// has BREE, confirm is java file
				if (needsBree) {
					report.setType(ReportType.INFO);
					report.setCheckResult(bree);
				} else {
					report.setType(ReportType.BAD_GUY);
					report.setCheckResult("None Java with BREE: " + bree);
				}
			} else {
				// no BREE, confirm is non-java
				if (needsBree) {
					report.setType(ReportType.BAD_GUY);
					report.setCheckResult("Java without BREE");
				}
			}
		} catch (SecurityException e) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=378764
			report.setType(ReportType.NOT_IN_TRAIN);
			report.setCheckResult("Invalid jar: " + child.getName());
		}
		if (report.getCheckResult() != null) {
			report.setTimeMs(System.currentTimeMillis());
			consumer.accept(report);
		}
	}

	private boolean needsBree(File child) {
		return exportsPackages(child) || containsJava(child);
	}

	private boolean exportsPackages(File child) {
		String entry = IUUtil.getBundleManifestEntry(child, Constants.EXPORT_PACKAGE);
		if (entry != null && !entry.isEmpty()) {
			return true;
		}
		return false;
	}

	private boolean containsJava(File jarfile) {
		// We assume the file is a 'jar' file.
		boolean containsJava = false;
		JarFile jar = null;
		try {
			jar = new JarFile(jarfile, false, ZipFile.OPEN_READ);
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".class")) {
					containsJava = true;
					break;
				} else if (entry.getName().endsWith(".jar")) {
					InputStream input = jar.getInputStream(entry);
					if (containsJava(input)) {
						containsJava = true;
						break;
					}
				}
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return containsJava;
	}

	private boolean containsJava(InputStream input) {
		// We assume the file is a 'jar' file.
		boolean containsJava = false;
		JarInputStream jarInputStream = null;
		try {
			jarInputStream = new JarInputStream(input);
			while (jarInputStream.available() > 0) {
				ZipEntry entry = jarInputStream.getNextEntry();
				if (entry != null) {
					if (entry.getName().endsWith(".class")) {
						containsJava = true;
						break;
					}
				}
			}

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
			if (jarInputStream != null) {
				try {
					jarInputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}

		}
		return containsJava;
	}

}
