package scala.tools.eclipse;

import scala.tools.eclipse.completion.CompletionTests;
import scala.tools.eclipse.hyperlinks.HyperlinkDetectorTests;
import scala.tools.eclipse.lexical.ScalaDocumentPartitionerTest;
import scala.tools.eclipse.lexical.ScalaPartitionTokeniserTest;
import scala.tools.eclipse.occurrences.OccurrencesFinderTest;
import scala.tools.eclipse.structurebuilder.StructureBuilderTest;
import scala.tools.eclipse.ui.TestScalaIndenter;
import scala.tools.eclipse.wizards.*;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * To run this class DO NOT FORGET to set the config.ini in the  "configuration" tab.
 * @author ratiu
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  ScalaDocumentPartitionerTest.class,
  ScalaPartitionTokeniserTest.class,
  ImportSupportTest.class,
  QualifiedNameSupportTest.class,
  HyperlinkDetectorTests.class,
  OccurrencesFinderTest.class,
  StructureBuilderTest.class,
  CompletionTests.class
})
class TestsSuite { }