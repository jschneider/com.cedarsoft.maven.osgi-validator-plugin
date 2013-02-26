package com.cedarsoft.osgi.validator;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Validates the package structure for usage with OSGi.
 * This plugin verifies whether the groupId and artifactId are reflected by the package names.
 * This ensures that no duplicate packages can be exported.
 *
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidatorMojo extends SourceFolderAwareMojo {
  public static final String MAVEN_PLUGIN_SUFFIX = "-maven-plugin";
  /**
   * Whether the build shall fail if a validation is detected
   */
  @Parameter(defaultValue = "${fail}", property = "osgi-validation.fail")
  private boolean fail = true;

  /**
   * The source directories containing the test sources to be compiled.
   */
  @Parameter( defaultValue = "${skipped.files}", property = "skipped.files" )
  protected List<String> skippedFiles = new ArrayList<>();

  /**
   * The prohibited package parts
   */
  @Parameter
  protected Set<String> prohibitedPackages= ImmutableSet.of("internal");

  @Parameter
  protected Set<String> packagePartsToSkip = ImmutableSet.of( "commons" );

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if ("pom".equals(mavenProject.getPackaging())) {
      getLog().info("Skipping for packaging \"pom\"");
      return;
    }

    getLog().info("Validating OSGi-stuff");

    validatePackages();
    validateImportedPackages();
  }

  private void validateImportedPackages() throws MojoFailureException {
    File manifestFile = new File( new File( classesDir, "META-INF" ), "MANIFEST.MF" );

    if ( !manifestFile.exists() ) {
      getLog().info( "No MANIFEST.MF found" );
      return;
    }

    getLog().info( "Validating " + manifestFile.getAbsolutePath() );

    try {
      try ( FileInputStream is = new FileInputStream( manifestFile ) ) {
        Manifest manifest = new Manifest( is );


        boolean containsError = false;
        Attributes mainAttributes = manifest.getMainAttributes();

        @Nullable String exportPackage = mainAttributes.getValue( "Export-Package" );
        if ( exportPackage != null ) {
          Iterable<String> packages = Splitter.on( ',' ).split( exportPackage );
          for ( String packageName : packages ) {
            for ( String prohibitedPackage : prohibitedPackages ) {
              if ( packageName.contains( prohibitedPackage ) ) {
                getLog().error( "Prohibited package exported: " + packageName );
                containsError = true;
              }
            }
          }
        }

        @Nullable String importPackage = mainAttributes.getValue( "Import-Package" );
        if ( importPackage != null ) {
          Iterable<String> packages = Splitter.on( ',' ).split( importPackage );
          for ( String packageName : packages ) {
            for ( String prohibitedPackage : prohibitedPackages ) {
              if ( packageName.contains( prohibitedPackage ) ) {
                getLog().error( "Prohibited package imported: " + packageName );
                containsError = true;
              }
            }
          }
        }

        if ( containsError ) {
          throw new MojoFailureException( "Invalid package export/import" );
        }
      }
    } catch ( IOException e ) {
      throw new MojoFailureException( "Could not read manifest", e );
    }
  }

  private void validatePackages() throws MojoExecutionException {
    Collection<String> problematicFiles = new ArrayList<String>();

    Set<String> allowedPrefixes = createAllowedPrefixes();
    getLog().info( "Allowed prefixes: " + allowedPrefixes );

    getLog().info("Source Roots:");
    getLog().debug( "Skipped Files: " + skippedFiles );

    for (String sourceRoot : getSourceRoots()) {
      getLog().info("\t" + sourceRoot);

      File sourceRootDir = new File(sourceRoot);

      if ( !sourceRootDir.isDirectory() ) {
        getLog().info( "Skipping <" + sourceRoot + ">: Is not a directory." );
        continue;
      }
      Collections.addAll( problematicFiles, validate( sourceRootDir, allowedPrefixes, skippedFiles ) );
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

  private static String[] validate(@Nonnull File sourceRoot, @Nonnull Set<String> allowedPrefixes, @Nonnull Collection<? extends String> skippedFiles) {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir( sourceRoot );
    scanner.setIncludes( new String[]{"**/*.java"} );

    Set<String> excludes = Sets.newHashSet();
    excludes.addAll( skippedFiles );

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


    return createAllowedPrefixes( groupId, artifactId, packagePartsToSkip );
  }

  protected static Set<String> createAllowedPrefixes( @Nonnull String groupId, @Nonnull String artifactId, @Nonnull Set<String> packagePartsToSkip ) {
    List<String> possibleIds = createPossibleIds( groupId + "." + artifactId, packagePartsToSkip );


    //Now create all combinations
    Set<String> allowedPrefixes = new HashSet<>();

    for ( String possibleId : possibleIds ) {
      allowedPrefixes.add( convertPackageToFile( possibleId ) );
    }

    //Remove duplicates
    for ( String current : new ArrayList<>( allowedPrefixes ) ) {
      List<String> idParts = Lists.newArrayList( Splitter.on( "/" ).split( current ) );
      Collection<String> partsAsSet = Sets.newLinkedHashSet( idParts );

      if ( idParts.size() > partsAsSet.size() ) {
        allowedPrefixes.add( Joiner.on( "/" ).join( partsAsSet ) );
      }
    }

    return allowedPrefixes;
  }

  @Deprecated
  @Nonnull
  static List<String> createPossibleIds(@Nonnull String id) {
    return createPossibleIds( id, ImmutableSet.of( "commons" ) );
  }

  @Nonnull
  static List<String> createPossibleIds(@Nonnull String id, @Nonnull Iterable<? extends String> partsToSkip) {
    List<String> ids = new ArrayList<>();
    ids.add(id);

    if (id.endsWith(MAVEN_PLUGIN_SUFFIX)) {
      ids.add(id.substring(0, id.indexOf(MAVEN_PLUGIN_SUFFIX)));
    }

    for ( String partToSkip : partsToSkip ) {
      {
        String skipped = skip( id, "." + partToSkip );
        if ( skipped != null ) {
          ids.add( skipped );
        }
      }

      {
        String skipped = skip( id, "-" + partToSkip );
        if ( skipped != null ) {
          ids.add( skipped );
        }
      }

      {
        String skipped = skip( id, partToSkip + "-" );
        if ( skipped != null ) {
          ids.add( skipped );
        }
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

  @Deprecated
  @Nonnull
  public static String createAllowedPrefix(@Nonnull String groupId, @Nonnull String artifactId) {
    String relevantArtifactId;
    if (artifactId.endsWith(MAVEN_PLUGIN_SUFFIX)) {
      relevantArtifactId = artifactId.substring(0, artifactId.indexOf(MAVEN_PLUGIN_SUFFIX));
    } else {
      relevantArtifactId = artifactId;
    }

    return convertPackageToFile( groupId + "." + relevantArtifactId );
  }

  @Nonnull
  private static String convertPackageToFile( @Nonnull String packageName ) {
    Splitter splitter = Splitter.on(new PackageSeparatorCharMatcher());
    List<String> partsList = Lists.newArrayList(splitter.split(packageName));
    return Joiner.on(File.separator).join(partsList);
  }

  private static class PackageSeparatorCharMatcher extends CharMatcher {
    @Override
    public boolean matches(char c) {
      return c == '.' || c == '-';
    }
  }
}
