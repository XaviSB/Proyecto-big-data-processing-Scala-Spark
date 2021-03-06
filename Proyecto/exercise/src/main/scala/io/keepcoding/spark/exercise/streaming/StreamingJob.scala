package io.keepcoding.spark.exercise.streaming

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import org.apache.spark.sql.{DataFrame, SparkSession}

case class AntennaMessage(timestamp: Timestamp, id: String, metric: String, value: Long)

trait StreamingJob {

  val spark: SparkSession

  def readFromKafka(kafkaServer: String, topic: String): DataFrame // igual que el proyecto final. Tendremos que cambiar el topic del que leemos.

  def parserJsonData(dataFrame: DataFrame): DataFrame // igual para el proyecto final

  def readAntennaMetadata(jdbcURI: String, jdbcTable: String, user: String, password: String): DataFrame // nos vale para el proyecto final. En lugar de antena, leemos de usuario. Cambiamos el nombre de la def.

  def enrichAntennaWithMetadata(antennaDF: DataFrame, metadataDF: DataFrame): DataFrame // valdrá para el proyecto final

  def countAntennaBytes(dataFrame: DataFrame): DataFrame

  def countUserBytes(dataFrame: DataFrame): DataFrame

  def countAppBytes(dataFrame: DataFrame): DataFrame

  def writeToJdbc(dataFrame: DataFrame, jdbcURI: String, jdbcTable: String, user: String, password: String): Future[Unit] //esto es genérico

  def writeToStorage(dataFrame: DataFrame, storageRootPath: String): Future[Unit] //esto es generico

  def run(args: Array[String]): Unit = {
    val Array(kafkaServer, topic, jdbcUri, jdbcMetadataTable, aggJdbcTable, jdbcUser, jdbcPassword, storagePath) = args
    println(s"Running with: ${args.toSeq}")

    val kafkaDF = readFromKafka(kafkaServer, topic)
    val antennaDF = parserJsonData(kafkaDF)
    val metadataDF = readAntennaMetadata(jdbcUri, jdbcMetadataTable, jdbcUser, jdbcPassword)
    val antennaMetadataDF = enrichAntennaWithMetadata(antennaDF, metadataDF)
    val storageFuture = writeToStorage(antennaDF, storagePath)
    val aggByCoordinatesDF = countAntennaBytes(antennaMetadataDF)
    val aggByCoordinatesDF2 = countUserBytes(antennaMetadataDF)
    val aggByCoordinatesDF3 = countAppBytes(antennaMetadataDF)
    val aggFuture = writeToJdbc(aggByCoordinatesDF, jdbcUri, aggJdbcTable, jdbcUser, jdbcPassword)

    Await.result(Future.sequence(Seq(aggFuture, storageFuture)), Duration.Inf)

    spark.close()
  }

}
