package kamon.metrics

/*
 * =========================================================================================
 * Copyright © 2013-2014 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

import com.typesafe.config.Config
import org.HdrHistogram.HdrRecorder

case class JvmMetrics(name: String) extends MetricGroupIdentity {
  val category = JvmMetrics
}

object JvmMetrics extends MetricGroupCategory {
  val name = "system"

  case object MaximumPoolSize extends MetricIdentity { val name, tag = "maximum-pool-size" }
  case object RunningThreadCount extends MetricIdentity { val name, tag = "running-thread-count" }
  case object QueueTaskCount extends MetricIdentity { val name, tag = "queued-task-count" }
  case object PoolSize extends MetricIdentity { val name, tag = "pool-size" }

  case class JvmMetricRecorder(maximumPoolSize: MetricRecorder, runningThreadCount: MetricRecorder, queueTaskCount: MetricRecorder, poolSize: MetricRecorder)
      extends MetricGroupRecorder {

    def collect: MetricGroupSnapshot = {
      JvmMetricSnapshot(maximumPoolSize.collect(), runningThreadCount.collect(), queueTaskCount.collect(), poolSize.collect())
    }
  }

  case class JvmMetricSnapshot(maximumPoolSize: MetricSnapshotLike, runningThreadCount: MetricSnapshotLike, queueTaskCount: MetricSnapshotLike, poolSize: MetricSnapshotLike)
      extends MetricGroupSnapshot {

    val metrics: Map[MetricIdentity, MetricSnapshotLike] = Map(
      (MaximumPoolSize -> maximumPoolSize),
      (RunningThreadCount -> runningThreadCount),
      (QueueTaskCount -> queueTaskCount),
      (PoolSize -> poolSize))
  }

  val Factory = new MetricGroupFactory {
    type GroupRecorder = JvmMetricRecorder

    def create(config: Config): JvmMetricRecorder = {
      val settings = config.getConfig("precision.jvm")

      val threadCountConfig = extractPrecisionConfig(settings.getConfig("maximum-pool-size"))
      val RunningThreadCountConfig = extractPrecisionConfig(settings.getConfig("running-thread-count"))
      val QueueTaskCountConfig = extractPrecisionConfig(settings.getConfig("queued-task-count"))
      val PoolSizeConfig = extractPrecisionConfig(settings.getConfig("pool-size"))

      new JvmMetricRecorder(
        HdrRecorder(threadCountConfig.highestTrackableValue, threadCountConfig.significantValueDigits, Scale.Unit),
        HdrRecorder(RunningThreadCountConfig.highestTrackableValue, RunningThreadCountConfig.significantValueDigits, Scale.Unit),
        HdrRecorder(QueueTaskCountConfig.highestTrackableValue, QueueTaskCountConfig.significantValueDigits, Scale.Unit),
        HdrRecorder(PoolSizeConfig.highestTrackableValue, PoolSizeConfig.significantValueDigits, Scale.Unit))
    }
  }
}

