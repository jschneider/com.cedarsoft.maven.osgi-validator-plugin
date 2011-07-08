package com.cedarsoft.osgi.validator;

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

  @Test
  public void testBasic() throws Exception {
    ValidatorMojo mojo1 = createMojo("basic");

    //    assertNotNull( mojo.projectArtifact );
    //    assertNotNull( mojo.outputDirectory );
    //    assertNotNull( mojo.domainSourceFilePattern );
    //    assertTrue( mojo.domainSourceFilePattern.length() > 0 );
    //
    //    assertNotNull( mojo.getTestOutputDirectory() );
    //    assertNotNull( mojo.getOutputDirectory() );
    //    assertNotNull( mojo.getResourcesOutputDirectory() );
    //    assertNotNull( mojo.getTestResourcesOutputDirectory() );

    ValidatorMojo mojo = mojo1;
    assertThat(mojo).isNotNull();

    //    assertEquals( 2, mojo.getExcludes().size() );
    //    assertTrue( mojo.outputDirectory.getAbsolutePath(), mojo.outputDirectory.getAbsolutePath().endsWith( "target/test/unit/target/out" ) );
    //    assertTrue( mojo.testOutputDirectory.getAbsolutePath(), mojo.testOutputDirectory.getAbsolutePath().endsWith( "target/test/unit/target/test-out" ) );
    mojo.execute();
    //
    //    assertEquals( SerializerGeneratorMojo.Target.STAX_MATE, mojo.getDialect() );
    //
    //    assertSerializers( mojo );
    //    assertTests( mojo );
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
