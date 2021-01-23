package com.cloudcover

import org.apache.spark.h2o.{H2OConf, H2OContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import ai.h2o.sparkling.ml.algos._

object FraudPredictor extends App {

  val currentDirectory = new java.io.File(".").getCanonicalPath
  System.setProperty("hadoop.home.dir","C:\\hadoop" )

  println("*** Set up SparkSession")
  val spark = SparkSession
    .builder
    .appName("FraudPredictor")
    .config("spark.master", "local[*]")
    .config("spark.driver.bindAddress", "localhost")
    .config("spark.sql.autoBroadcastJoinThreshold", "-1")
    .config("spark.locality.wait", "0")
    .config("spark.ext.h2o.repl.enabled","false")
    .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")

  println("*** H2O Configuration and Context")
  val h2oConf = new H2OConf(spark)
    .setH2ONodeLogLevel("WARN")
    .setH2OClientLogLevel("WARN")
    .set("spark.ui.enabled", "false")
    .set("spark.locality.wait", "0")

  println("*** Set Up H2O Context")
  val h2oContext = H2OContext.getOrCreate(spark, h2oConf)

  val df = spark
    .read
    .format("csv")
    .option("header", "true")
    .option("inferSchema", "true")
    .load(currentDirectory + "/Data/bq-results-20210123-142626-ayxy0j76x3x5.csv") //Change Path for your file
    .persist()
  df.show(false)
  df.printSchema()

  println("*** Class dstribution")
  df.groupBy("class").count().show(false)

  println("*** Split data into pred, test and training")
  val Array(trainingDf, testDf, predDf) = df.drop("Time").randomSplit(Array(0.8, 0.1, 0.1))
  val trainH2ODf = trainingDf.withColumn("class", expr("cast(class as string)")).persist()
  val testH2ODf = testDf.withColumn("class", expr("cast(class as string)")).persist()
  val predH2ODf = predDf.withColumn("class", expr("cast(class as string)")).persist()

  println("*** Distribution of each dataframe")
  println("trainH2ODf - ")
  trainH2ODf.groupBy("class").count().show(false)
  println("testH2ODf - ")
  testH2ODf.groupBy("class").count().show(false)
  println("predH2ODf - ")
  predH2ODf.groupBy("class").count().show(false)

  val algo = new H2ODRF()
      .setLabelCol("class")
      .setConvertInvalidNumbersToNa(true)

  val model = algo.fit(trainH2ODf)
  println(s"*** Model Metrics \n ${model.getCurrentMetrics()}")

  val testScoredDf = model.transform(testH2ODf)
  val predScoredDf = model.transform(predH2ODf)

  //testScoredDf.printSchema()
  println("*** Confusion Matrix for Test Frame")
  testScoredDf.groupBy("class", "prediction").count().show(false)

  println("*** Confusion Matrix for Pred Frame")
  predScoredDf.groupBy("class", "prediction").count().show(false)

  spark.stop()
  h2oContext.stop(stopSparkContext = true)
}
