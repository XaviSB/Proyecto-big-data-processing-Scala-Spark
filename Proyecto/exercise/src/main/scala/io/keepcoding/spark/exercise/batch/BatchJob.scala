package io.keepcoding.spark.exercise.batch

import java.sql.Timestamp
import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import org.apache.spark.sql.{DataFrame, SparkSession}

case class AntennaMessage(year: Int, month: Int, day: Int, hour: Int, timestamp: Timestamp, id: String, metric: String, value: Long)

trait BatchJob {

  val spark: SparkSession

  def readFromStorage(storagePath: String, filterDate: OffsetDateTime): DataFrame

  def readAntennaMetadata(jdbcURI: String, jdbcTable: String, user: String, password: String): DataFrame

  def enrichAntennaWithMetadata(antennaDF: DataFrame, metadataDF: DataFrame): DataFrame

  def countAntennaBytes(dataFrame: DataFrame): DataFrame

  def countEmailBytes(dataFrame: DataFrame): DataFrame

  def countAppBytes(dataFrame: DataFrame): DataFrame

  def totalBytesUserMail(dataFrame: DataFrame): DataFrame

  def writeToJdbc(dataFrame: DataFrame, jdbcURI: String, jdbcTable: String, user: String, password: String): Unit

  def writeToStorage(dataFrame: DataFrame, storageRootPath: String): Unit

  def run(args: Array[String]): Unit = {
    val Array(filterDate, storagePath, jdbcUri, jdbcMetadataTable, aggJdbcTable, aggJdbcErrorTable, aggJdbcPercentTable, jdbcUser, jdbcPassword) = args
    println(s"Running with: ${args.toSeq}")

    val antennaDF = readFromStorage(storagePath, OffsetDateTime.parse(filterDate))
    val metadataDF = readAntennaMetadata(jdbcUri, jdbcMetadataTable, jdbcUser, jdbcPassword)
    val antennaMetadataDF = enrichAntennaWithMetadata(antennaDF, metadataDF).cache()
    val aggByCoordinatesDF = countAntennaBytes(antennaMetadataDF)
    val aggByCoordinatesDF2 = countEmailBytes(antennaMetadataDF)
    val aggByCoordinatesDF3 = countAppBytes(antennaMetadataDF)
    val aggByCoordinatesDF4 = totalBytesUserMail(antennaMetadataDF)


    writeToJdbc(aggByCoordinatesDF, jdbcUri, aggJdbcTable, jdbcUser, jdbcPassword)
    writeToJdbc(aggByCoordinatesDF2, jdbcUri, aggJdbcPercentTable, jdbcUser, jdbcPassword)
    writeToJdbc(aggByCoordinatesDF3, jdbcUri, aggJdbcErrorTable, jdbcUser, jdbcPassword)
    writeToJdbc(aggByCoordinatesDF4, jdbcUri, aggJdbcErrorTable, jdbcUser, jdbcPassword)

    writeToStorage(antennaDF, storagePath)

    spark.close()
  }

}
