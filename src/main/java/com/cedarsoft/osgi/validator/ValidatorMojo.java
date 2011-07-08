package com.cedarsoft.osgi.validator;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

import javax.annotation.Nonnull;
import java.io.File;
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
  public static final String MAVEN_PLUGIN_SUFFIX = "-maven-plugin";
  /**
   * Whether the build shall fail if a validation is detected
   *
   * @parameter expression="${fail}"
   */
  private boolean fail = true;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Validating OSGI-stuff");

    List<String> problematicFiles = new ArrayList<String>();

    String allowedPrefix = createPackageName();
    getLog().debug("Allowed prefix: " + allowedPrefix);

    getLog().info("Source Roots:");
    for (String sourceRoot : getSourceRoots()) {
      getLog().info("\t" + sourceRoot);

      File sourceRootDir = new File(sourceRoot);
      Collections.addAll(problematicFiles, validate(sourceRootDir, allowedPrefix));
    }

    if (problematicFiles.isEmpty()) {
      getLog().info("No problematic files found");
      return;
    }

    if (fail) {
      getLog().error("Found files within a problematic package:");
      for (String problematicFile : problematicFiles) {
        getLog().error("  " + problematicFile);
      }
      throw new MojoExecutionException("There exist " + problematicFiles.size() + " files that seem to be placed within a problematic package");
    } else {
      getLog().warn("Found files within a problematic package:");
      for (String problematicFile : problematicFiles) {
        getLog().warn("  " + problematicFile);
      }
    }
  }

  private String[] validate(@Nonnull File sourceRoot, @Nonnull String allowedPrefix) {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(sourceRoot);
    scanner.setIncludes(new String[]{"**/*.java"});
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
    String relevantArtifactId = artifactId; //-maven-plugin
    if (artifactId.endsWith(MAVEN_PLUGIN_SUFFIX)) {
      relevantArtifactId = artifactId.substring(0, artifactId.indexOf("-maven-plugin"));
    } else {
      relevantArtifactId = artifactId;
    }


    Splitter splitter = Splitter.on(new PackageSeparatorCharMatcher());

    List<String> partsList = Lists.newArrayList(splitter.split(groupId));
    partsList.addAll(Lists.<String>newArrayList(splitter.split(relevantArtifactId)));


    return Joiner.on(File.separator).join(partsList);
  }

  private static class PackageSeparatorCharMatcher extends CharMatcher {
    @Override
    public boolean matches(char c) {
      return c == '.' || c == '-';
    }
  }
}
