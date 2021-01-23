package com.cloudcover

import org.apache.spark.h2o.{H2OConf, H2OContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import ai.h2o.sparkling.ml.algos._
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.feature.VectorAssembler

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
  println("train data - ")
  trainH2ODf.groupBy("class").count().show(false)
  println("test data - ")
  testH2ODf.groupBy("class").count().show(false)
  println("pred data - ")
  predH2ODf.groupBy("class").count().show(false)

  println("*** Run H2O Distributed Random Forest")
  val h2oAlgo = new H2ODRF()
      .setLabelCol("class")
      .setConvertInvalidNumbersToNa(true)

  val h2oModel = h2oAlgo.fit(trainH2ODf)
  println(s"*** Model Metrics \n ${h2oModel.getCurrentMetrics()}")

  val testScoredH2ODf = h2oModel.transform(testH2ODf)
  val predScoredH2ODf = h2oModel.transform(predH2ODf)

  //testScoredDf.printSchema()
  println("*** H2O Results - Confusion Matrix for Test Frame")
  testScoredH2ODf
    .groupBy("class", "prediction")
    .count()
    .orderBy("class", "prediction")
    .show(false)

  println("*** H2O Results - Confusion Matrix for Pred Frame")
  predScoredH2ODf
    .groupBy("class", "prediction")
    .count()
    .orderBy("class", "prediction")
    .show(false)

  println("*** Run Spark ML Random Forrest")
  val vectorAssembler = new VectorAssembler()
    .setInputCols(trainingDf.drop("class").columns)
    .setOutputCol("assembledFeatures")

  val vectoredTrainDf = vectorAssembler.transform(trainingDf)

  val sparkAlgo = new RandomForestClassifier()
    .setLabelCol("Class")
    .setFeaturesCol("assembledFeatures")
    .setNumTrees(20)

  val pipeline = new Pipeline()
    .setStages(Array(vectorAssembler, sparkAlgo))

  val sparkModel = pipeline.fit(trainingDf)

  val testScoredSparkMLDf = sparkModel.transform(testDf)
  val predScoredSparkMLDf = sparkModel.transform(predDf)

  //testScoredDf.printSchema()
  println("*** Confusion Matrix for Test Frame")
  testScoredSparkMLDf
    .groupBy("class", "prediction")
    .count()
    .orderBy("class", "prediction")
    .show(false)

  println("*** Confusion Matrix for Pred Frame")
  predScoredSparkMLDf
    .groupBy("class", "prediction")
    .count()
    .orderBy("class", "prediction")
    .show(false)

  spark.stop()
  h2oContext.stop(stopSparkContext = true)
}
