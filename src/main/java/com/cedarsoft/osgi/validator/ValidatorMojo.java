package com.cedarsoft.osgi.validator;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validates the package structure for usage with OSGi.
 * This plugin verifies whether the groupId and artifactId are reflected by the package names.
 * This ensures that no duplicate packages can be exported.
 *
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 * @goal validate
 * @phase process-sources
 */
public class ValidatorMojo extends SourceFolderAwareMojo {
  /**
   * Whether the build shall fail if a validation is detected
   *
   * @parameter expression="${fail}"
   */
  private boolean fail;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Validating OSGI-stuff");

    List<String> problematicFiles = new ArrayList<String>();


    getLog().info("Source Roots:");
    for (String sourceRoot : getSourceRoots()) {
      getLog().info("\t" + sourceRoot);

      File sourceRootDir = new File(sourceRoot);
      Collections.addAll(problematicFiles, validate(sourceRootDir));
    }


    if (problematicFiles.isEmpty()) {
      getLog().info("No problematic files found");
      return;
    }

    getLog().warn("Found files within a problematic package:");
    for (String problematicFile : problematicFiles) {
      getLog().warn("  " + problematicFile);
    }

    if (fail) {
      throw new MojoExecutionException("There exist " + problematicFiles.size() + " files that seem to be placed within a problematic package");
    }
  }

  private String[] validate(@Nonnull File sourceRoot) {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(sourceRoot);
    scanner.setIncludes(new String[]{"**/*.java"});

    String allowedPrefix = createPackageName();
    scanner.setExcludes(new String[]{allowedPrefix + "/**"});

    scanner.scan();
    return scanner.getIncludedFiles();
  }

  @Nonnull
  private String createPackageName() {
    String groupId = getProject().getGroupId();
    String artifactId = getProject().getArtifactId();

    return createPackageName(groupId, artifactId);
  }

  @Nonnull
  public static String createPackageName(@Nonnull String groupId, @Nonnull String artifactId) {
    Splitter splitter = Splitter.on(new PackageSeparatorCharMatcher());

    List<String> partsList = Lists.newArrayList(splitter.split(groupId));
    partsList.addAll(Lists.<String>newArrayList(splitter.split(artifactId)));


    return Joiner.on(File.separator).join(partsList);
  }

  private static class PackageSeparatorCharMatcher extends CharMatcher {
    @Override
    public boolean matches(char c) {
      return c == '.' || c == '-';
    }
  }
}
