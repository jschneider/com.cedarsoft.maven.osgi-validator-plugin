package com.cedarsoft.osgi.validator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.junit.*;

import javax.annotation.Nonnull;
import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 */
public class ValidatorMojoTest extends AbstractMojoTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LoggerManager loggerManager = getContainer().lookup(LoggerManager.class);
    loggerManager.setThreshold(Logger.LEVEL_DEBUG);
  }

  public void testBasic() throws Exception {
    ValidatorMojo mojo = createMojo("basic");

    assertThat(mojo).isNotNull();

    try {
      mojo.execute();
      fail("Where is the Exception");
    } catch (MojoExecutionException ignore) {
    }
  }

  @Nonnull
  private ValidatorMojo createMojo(@Nonnull String name) throws Exception {
    File testPom = new File(getBasedir(), "src/test/resources/com/cedarsoft/osgi/validator/test/basic.xml");
    assertTrue(testPom.exists());
    ValidatorMojo mojo = (ValidatorMojo) lookupMojo("validate", testPom);

    assertNotNull(mojo);
    MavenProjectStub projectStub = new MavenProjectStub();
    projectStub.setGroupId("com.cedarsoft.osgi-validator");
    projectStub.setArtifactId("test");

    mojo.mavenProject = projectStub;

    //    cleanUp( mojo );
    return mojo;
  }

  public void testPackagePath() throws Exception {
    assertThat(ValidatorMojo.createPackageName("com.cedarsoft", "test")).isEqualTo("com/cedarsoft/test");
    assertThat(ValidatorMojo.createPackageName("com.cedarsoft", "test-asdf")).isEqualTo("com/cedarsoft/test/asdf");
    assertThat(ValidatorMojo.createPackageName("com.cedarsoft-ear", "test-asdf")).isEqualTo("com/cedarsoft/ear/test/asdf");
  }
}
