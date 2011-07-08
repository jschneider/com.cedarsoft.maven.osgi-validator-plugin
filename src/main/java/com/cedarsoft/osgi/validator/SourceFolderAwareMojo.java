package com.cedarsoft.osgi.validator;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 */
public abstract class SourceFolderAwareMojo extends AbstractMojo {
  /**
   * The source directories containing the sources to be compiled.
   *
   * @parameter default-value="${project.compileSourceRoots}"
   * @required
   * @readonly
   */
  protected List<String> sourceRoots;
  /**
   * The source directories containing the test sources to be compiled.
   *
   * @parameter default-value="${project.testCompileSourceRoots}"
   * @required
   * @readonly
   */
  protected List<String> testSourceRoots;

  /**
   * The list of resources.
   *
   * @parameter default-value="${project.resources}"
   * @required
   * @readonly
   */
  private List<Resource> resources;

  /**
   * The list of test resources
   *
   * @parameter expression="${project.testResources}"
   * @required
   * @readonly
   */
  private List<Resource> testResources;

  /**
   * The maven session
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  protected MavenProject mavenProject;

  protected MavenProject getProject() {
    return mavenProject;
  }

  @Nonnull
  public List<String> getSourceRoots() {
    return Collections.unmodifiableList(sourceRoots);
  }
}
