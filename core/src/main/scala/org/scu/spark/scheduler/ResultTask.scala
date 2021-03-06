package org.scu.spark.scheduler

import org.scu.spark.broadcast.Broadcast
import org.scu.spark.{Accumulator, Partition}

/**
 * ResultTask 将结果返回个Driver
 *
 * Created by bbq on 2015/12/9
 */
private[spark] class ResultTask[T,U](
                                    stageId:Int,
                                    stageAttemptId:Int,
                                    taskBianry:Broadcast[Array[Byte]],
                                    partition:Partition,
                                    locs:Seq[TaskLocation],
                                    val outputId : Int,
                                    internalAccumulators:Seq[Accumulator[Long]]
                                      ) extends Task[U](stageId,stageAttemptId,partition.index,internalAccumulators) with Serializable{

}
