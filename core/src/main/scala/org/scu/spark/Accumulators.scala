package org.scu.spark

import java.util.concurrent.atomic.AtomicLong

class Accumulable[R,T] private[spark](
                                     initialValue:R,
                                     param:AccumulableParam[R,T],
                                     val name : Option[String],
                                     internal : Boolean
                                       ) extends Serializable{
  val id  = Accumulators.newId()

  /**当前的值*/
  @volatile @transient private var value_ : R = initialValue

  val zero = param.zero(initialValue)

  /**时候在主节点上*/
  private var deserialized = false

  /**如果是internal的时候，需要向driver发送心跳信息*/
  private[spark] def isInternal:Boolean = internal

  def += (term:T) { value_ = param.addAccumulator(value_,term) }

  def add(term:T) { value_ = param.addAccumulator(value_,term) }

  def ++= (term:R) { value_ = param.addInPlace(value_,term) }

  def merge (term:R) {value_ = param.addInPlace(value_,term) }

  /** 在task任务中是不能获取全局值的*/
  def value:R={
    if(!deserialized)
      value_
    else
      throw new UnsupportedOperationException("Can't read accumulator value in task")
  }

  def localValue : R = value_

  /**重新设置value，只有driver才可以 */
  def value_= (newValue:R): Unit ={
    value = newValue
  }

  /**重新设置value，只有driver才可以设置*/
  def setValue(newValue:R): Unit ={
    value = newValue
  }

  override def toString :String = if (value_ == null) "null" else value_.toString

}


/**
 * 累加器的帮助类，实现如何对元素进行相加，以及对两个相加后的集合进行合并操作。类似于aggregate
 * @tparam R 最终的计算的数据类型
 * @tparam T 中间结果，部分相加的结果
 */
trait AccumulableParam[R,T] extends Serializable{

  /**
   * 添加一个中间结果类型
   * 例如：我们需要将所有的List[Int]添加到Int中，就要使用addAccumulator(Int,List[Int])
   */
  def addAccumulator(r:R,t:T):R

  /**
   * 将两个计算结果类型的数据合并
   */
  def addInPlace(r1:R,r2:R):R

  /**
   * 返回初始化值，如果R的类型是List[Int](n),那么就初始化为值全为0的N维List
   */
  def zero(initialValue:R):R
}

/**
 * spark中的累加器实现：精简版实现，当中间结果和最终结果的类型相同时。
 * 可以用来当作计数器。spark原生支持数字类型相加，也可以自己实现。
 *
 * 但我们是用Accumulator的时候，是用SparkContext.accumulator。
 * Task在运行的时候可以相加，但是不能读取数据，最终的数据只能由master读取
 *
 *
 * Created by bbq on 2015/11/27
 */
object Accumulators {

  private val lastId = new AtomicLong(0)

  def newId():Long = lastId.getAndIncrement()
}

class Accumulator[T] private[spark](
                                   @transient private[spark] val initialValue : T,
                                   parm : AccumulableParam[T,T],
                                   name:Option[String],
                                   internal:Boolean
                                     )extends Accumulable[T,T](initialValue,parm,name,internal){
  def this(initialValue:T,param:AccumulatorParam[T],name:Option[String])={
    this(initialValue,parm,name,false)
  }

  def this(initialValue:T,param:AccumulatorParam[T])={
    this(initialValue,parm,None,false)
  }

}

/**
 * R和T的类型相同
 * @tparam T 中间结果，部分相加的结果
 */
trait AccumulatorParam[T] extends AccumulableParam[T,T]{
  def addAccumulator(t1:T,t2:T):T={
    addInPlace(t1,t2)
  }
}

object AccumulatorParam{

}

//private[spark] object