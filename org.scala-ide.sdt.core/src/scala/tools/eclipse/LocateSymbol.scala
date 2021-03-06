/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse

import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.{ICodeAssist, IJavaElement, WorkingCopyOwner}
import org.eclipse.jface.text.{IRegion, ITextViewer}
import org.eclipse.jface.text.hyperlink.{AbstractHyperlinkDetector, IHyperlink}
import org.eclipse.jdt.internal.core.{ClassFile,Openable, SearchableEnvironment, JavaProject}
import org.eclipse.ui.texteditor.ITextEditor
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.internal.ui.javaeditor.{EditorUtility, JavaElementHyperlink}

import tools.nsc.symtab.Flags._
import scala.tools.nsc.io.AbstractFile

import javaelements.{ScalaSourceFile, ScalaClassFile, ScalaCompilationUnit}

trait LocateSymbol { self : ScalaPresentationCompiler => 

  def locate(sym : Symbol, scu : ScalaCompilationUnit): Option[(ScalaCompilationUnit, Int)] = {
    def find[T, V](arr : Array[T])(f : T => Option[V]) : Option[V] = {
      for(e <- arr) {
        f(e) match {
          case v@Some(_) => return v
          case None =>
        }
      }
      None
    }
    def findClassFile = {
      val packName = sym.enclosingPackage.fullName
      val project = scu.getJavaProject.asInstanceOf[JavaProject]
      val pfs = new SearchableEnvironment(project, null: WorkingCopyOwner).nameLookup.findPackageFragments(packName, false)
      if (pfs eq null) None else find(pfs) {
        val top = sym.toplevelClass
        val name = top.name + (if (top.isModule) "$" else "") + ".class"
        _.getClassFile(name) match {
          case classFile : ScalaClassFile => Some(classFile)
          case _ => None
        }
      }
    }
    (if (sym.sourceFile ne null) {
       val path = new Path(sym.sourceFile.path)
       val root = ResourcesPlugin.getWorkspace().getRoot()
       root.findFilesForLocation(path) match {
         case arr : Array[_] if arr.length == 1 =>
           ScalaSourceFile.createFromPath(arr(0).getFullPath.toString)
         case _ => findClassFile
       }
    } else findClassFile) flatMap { file =>
      (if (sym.pos eq NoPosition) {
        file.withSourceFile { (f, _) =>
          val pos = new Response[Position]
          askLinkPos(sym, f, pos)
//          askReload(scu, scu.getContents) // TODO: Find out why this was necessary
          pos.get.left.toOption
        } (None)
      } else Some(sym.pos)) flatMap { p =>
        if (p eq NoPosition) None else Some(file, p.point)
      }
    }
  }
}