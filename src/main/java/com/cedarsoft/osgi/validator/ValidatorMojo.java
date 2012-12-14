package com.cedarsoft.osgi.validator;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    if ("pom".equals(mavenProject.getPackaging())) {
      getLog().info("Skipping for packaging \"pom\"");
      return;
    }

    getLog().info("Validating OSGI-stuff");

    validatePackages();
  }

  private void validatePackages() throws MojoExecutionException {
    Collection<String> problematicFiles = new ArrayList<String>();

    Set<String> allowedPrefixes = createAllowedPrefixes();
    getLog().info("Allowed prefixes: " + allowedPrefixes);

    getLog().info("Source Roots:");
    for (String sourceRoot : getSourceRoots()) {
      getLog().info("\t" + sourceRoot);

      File sourceRootDir = new File(sourceRoot);

      if ( !sourceRootDir.isDirectory() ) {
        getLog().info( "Skipping <" + sourceRoot + ">: Is not a directory." );
        continue;
      }
      Collections.addAll( problematicFiles, validate( sourceRootDir, allowedPrefixes ) );
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

  private static String[] validate(@Nonnull File sourceRoot, @Nonnull Set<String> allowedPrefixes) {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir( sourceRoot );
    scanner.setIncludes( new String[]{"**/*.java"} );

    Set<String> excludes = Sets.newHashSet();
    for ( String allowedPrefix : allowedPrefixes ) {
      excludes.add( allowedPrefix + "/**" );
    }
    scanner.setExcludes( excludes.toArray( new String[excludes.size()] ) );

    scanner.scan();
    return scanner.getIncludedFiles();
  }

  @Nonnull
  private Set<String> createAllowedPrefixes() {
    String groupId = getProject().getGroupId();
    String artifactId = getProject().getArtifactId();


    List<String> possibleGroupIds = createPossibleIds(groupId);
    List<String> possibleArtifactIds = createPossibleIds(artifactId);


    //Now create all combinations
    Set<String> allowedPrefixes = new HashSet<String>();

    //Create all combinations
    for (String possibleGroupId : possibleGroupIds) {
      for (String possibleArtifactId : possibleArtifactIds) {
        allowedPrefixes.add(createPrefix(possibleGroupId, possibleArtifactId));
      }
    }

    //Remove duplicates
    for (String current : new ArrayList<String>(allowedPrefixes)) {
      List<String> idParts = Lists.newArrayList(Splitter.on("/").split(current));
      Collection<String> partsAsSet = Sets.newLinkedHashSet(idParts);

      if (idParts.size() > partsAsSet.size()) {
        allowedPrefixes.add(Joiner.on("/").join(partsAsSet));
      }
    }

    return allowedPrefixes;
  }

  static List<String> createPossibleIds(@Nonnull String id) {
    List<String> ids = new ArrayList<String>();
    ids.add(id);

    if (id.endsWith(MAVEN_PLUGIN_SUFFIX)) {
      ids.add(id.substring(0, id.indexOf(MAVEN_PLUGIN_SUFFIX)));
    }

    {
      String skipped = skip(id, ".commons");
      if (skipped != null) {
        ids.add(skipped);
      }
    }

    {
      String skipped = skip(id, "-commons");
      if (skipped != null) {
        ids.add(skipped);
      }
    }

    {
      String skipped = skip(id, "commons-");
      if (skipped != null) {
        ids.add(skipped);
      }
    }

    if (id.endsWith("s")) {
      ids.add(id.substring(0, id.length() - 1));
    }

    return ids;
  }

  @Nullable
  private static String skip(@Nonnull String id, @Nonnull String toSkip) {
    if (!id.contains(toSkip)) {
      return null;
    }

    int start = id.indexOf(toSkip);
    String first = id.substring(0, start);
    String second = id.substring(start + toSkip.length());

    return first + second;
  }

  @Nonnull
  public static String createAllowedPrefix(@Nonnull String groupId, @Nonnull String artifactId) {
    String relevantArtifactId;
    if (artifactId.endsWith(MAVEN_PLUGIN_SUFFIX)) {
      relevantArtifactId = artifactId.substring(0, artifactId.indexOf(MAVEN_PLUGIN_SUFFIX));
    } else {
      relevantArtifactId = artifactId;
    }

    return createPrefix(groupId, relevantArtifactId);
  }

  @Nonnull
  private static String createPrefix(@Nonnull String relevantGroupId, @Nonnull String relevantArtifactId) {
    Splitter splitter = Splitter.on(new PackageSeparatorCharMatcher());

    List<String> partsList = Lists.newArrayList(splitter.split(relevantGroupId));
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
