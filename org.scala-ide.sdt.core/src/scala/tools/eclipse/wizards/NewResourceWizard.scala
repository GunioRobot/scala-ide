/*
 * Copyright 2005-2010 LAMP/EPFL
 * @author Sean McDirmid
 */
// $Id$

package scala.tools.eclipse.wizards

import scala.collection.mutable.ArrayBuffer

import org.eclipse.core.resources.{ IFile, IFolder, IncrementalProjectBuilder, IProject, IResource }
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jdt.core.{ IPackageFragment, IPackageFragmentRoot }
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jface.wizard.WizardPage
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.{ GridData, GridLayout }
import org.eclipse.swt.widgets.{ Combo, Composite, Control, Group, Text }
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard

import scala.tools.nsc.util.Chars

import scala.tools.eclipse.{ ScalaPlugin, ScalaProject }
import scala.tools.eclipse.util.EclipseUtils._

trait NewResourceWizard extends BasicNewResourceWizard {
  def kind : String
  def adjective : String = ""
  def noun(toLower : Boolean) : String = {
    def f(str : String) = if (toLower) str.toLowerCase else str
    (f(adjective) match {
      case "" => ""
      case str => str + " "
    })+ f(kind)
  }
  val packages = new ArrayBuffer[IPackageFragment]
  var text : Text = _
  var choose : Combo = _
  class Page extends WizardPage("New Scala " + kind) {
    private def populate(iproject : IProject) : Unit = {
      for (pfr <- ScalaPlugin.plugin.getJavaProject(iproject).getAllPackageFragmentRoots if pfr.getKind == IPackageFragmentRoot.K_SOURCE ; child <- pfr.getChildren)
        child match {
          case pf : IPackageFragment => packages += pf
          case _ =>
        }
    }
    def createControl(parent : Composite) : Unit = try {
      val topLevel = new Composite(parent, SWT.NONE)
      val topLayout = new GridLayout()
        topLayout.verticalSpacing = 0;
        topLevel.setLayout(topLayout)
        topLevel.setFont(parent.getFont())

        setErrorMessage(null)
        setMessage(null)
        setControl(topLevel)
        var fail = false
        if (getSelection.size == 1 && (getSelection.getFirstElement match {
        case pkg : IPackageFragment => 
          packages += pkg
          true
        case _ => false  
        })) {} else {
          val i = getSelection.iterator
          while (i.hasNext) i.next match {
          case res : IResource => populate(res.getProject)
          case adaptable : IAdaptable => adaptable.adaptToSafe[IResource] foreach { res => populate(res.getProject) }
          case _ => 
          }
        }
        {
          packages.size match {
          case 0 => 
            setErrorMessage("Cannot create top-level Scala " + noun(true) + " without project or package selection.")
            return
          case 1 =>
            val pkgName0 = packages(0).getElementName
            val pkgName = if (pkgName0.length == 0) "default package" else "package \""+pkgName0+"\""
            setDescription("Create new top-level Scala " + noun(true) + " in "+pkgName+" of project \"" + packages(0).getResource.getProject.getName + "\"")
          case _ => 
            val group = label(topLevel, "Package:")
            choose = new Combo(group, SWT.SINGLE | SWT.READ_ONLY)
            addToGroup(group, choose)
            packages.foreach(choose add _.getElementName)
          }
          val group = label(topLevel, kind + " name:")
          text = new Text(group, SWT.LEFT + SWT.BORDER)
          addToGroup(group, text)
        }
    } catch {
    case ex => ScalaPlugin.plugin.logError(ex)
    }
  }
  private def label(parent : Composite, label : String) = {
    val group= new Group(parent, SWT.NONE);
    group.setText(label); 
    val layout = new GridLayout();
    layout.numColumns = 2;
    group.setLayout(layout);
    group.setFont(parent.getFont());
    val gd = new GridData(GridData.FILL_HORIZONTAL);
    group.setLayoutData(gd);
    group;
  }
  private def addToGroup(group : Group, control : Control) = {
    val gd = new GridData(GridData.FILL_HORIZONTAL);
    control.setLayoutData(gd);
    control.setFont(group.getFont());
  }
  private var mainPage : Page = _
  override def addPages = {
    super.addPages
    mainPage = new Page
    mainPage.setTitle("New Scala " + noun(false))
    mainPage.setDescription("Create new top-level Scala " + noun(true));
    addPage(mainPage)
  }
  override def init(workbench : IWorkbench, currentSelection: IStructuredSelection) = {
    super.init(workbench, currentSelection)
    setWindowTitle("New Scala  " + noun(false))
    setNeedsProgressMonitor(false)
  }
  protected def name = {
    var name = text.getText().trim()
    if (name.endsWith(".scala"))
      name = name.substring(0, name.length() - (".scala").length())
    name
  }
  protected def pkg = {
    assert(!packages.isEmpty)
    if (packages.size == 1) packages(0)
    else if (choose != null && choose.getSelectionIndex >= 0) packages(choose.getSelectionIndex)
    else null
  }
  override def performFinish : Boolean = try {
    val pkg = this.pkg
    val name = this.name
    if (pkg eq null) {
      mainPage.setErrorMessage("Must select an existing package.")
      return false
    }        
    val plugin = ScalaPlugin.plugin
    val project = plugin.getScalaProject(pkg.getResource.getProject)
    val nameOk = name.length != 0 && Chars.isIdentifierStart(name(0)) && (1 until name.length).forall(i => Chars.isIdentifierPart(name(i)))
      
    if (!nameOk) {
      mainPage.setErrorMessage("Not a valid name.")
      return false
    }
    val folder = pkg.getResource.asInstanceOf[IFolder]
    val file = folder.getFile(name + ".scala")
    if (file.exists) {
      mainPage.setErrorMessage("Resource with same name already exists.")
      return false
    }

    val pkgName = pkg.getElementName
    val pkgDecl = if (pkgName.length == 0) "" else "package "+pkgName+"\n\n"
    
    file.create(new java.io.StringBufferInputStream(
      pkgDecl +
      kind.toLowerCase + " " + name + " {\n" + body + "\n}\n"
    ), true, null)
    // force build!
    file.getProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null)
    getWorkbench.getActiveWorkbenchWindow match {
      case null =>
      case dw => dw.getActivePage match {
        case null =>
        case page => IDE.openEditor(page, file, true) 
      }
    }
    postFinish(project, file)
    
    true
  } catch {
  case ex => ScalaPlugin.plugin.logError(ex); false
  }
  protected def postFinish(project : ScalaProject, file : IFile) = {}
  
  
  protected def body : String = ""
}
