package org.xerial.snappy

import org.scalatest._
import xerial.core.log.Logger
import xerial.core.util.Timer

/**
 *
 */
trait SnappySpec
  extends WordSpec
  with Matchers
  with GivenWhenThen
  with OptionValues
  with BeforeAndAfter
  with Timer
  with Logger
{

  implicit def toTag(s:String) : Tag = Tag(s)

}
