/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse.util

/** A ref cell to a managed resource. Overwrite 'create' and 'destroy'.
 * 
 *  @note Important: 'create' is not allowed to throw any exceptions. Throwing
 *        an exception may lead to either NoSuchElementException, or an infinite
 *        loop if other threads wait while 'create' is in progress. Use a Cached[Option[T]] 
 *        in such a case.
 *      
 */
trait Cached[T] {
  private var inProgress = false
  private var elem : Option[T] = None
  
  def apply[U](op : T => U) : U = {
    val e = synchronized {
      elem match {
        case Some(t) => t
        case None => {
          while (!elem.isDefined) {
            try {
              while (inProgress)
                wait
            } catch {
              case ex : InterruptedException =>
                if (!elem.isDefined) {
                  throw ex
                }
            }
        
            if (!elem.isDefined) {
              inProgress = true
              try {
                val t = create
                elem = Some(t)
              } finally {
                inProgress = false
                notifyAll
              }
            }
          }
          elem.get
        }
      }
    }
    
    op(e)
  }
  
  def invalidate() {
    val oldElem = synchronized {
      val elem0 = elem
      elem = None
      elem0
    }
    
    oldElem match {
      case Some(t) => destroy(t)
      case _ =>
    }
  }

  /** Should not throw. */
  protected def create() : T

  protected def destroy(t : T)
}
