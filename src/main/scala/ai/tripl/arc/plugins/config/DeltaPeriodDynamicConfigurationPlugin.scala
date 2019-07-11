package ai.tripl.arc.plugins.config

import java.util
import java.sql.Date
import java.time.LocalDate
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.format.ResolverStyle
import scala.collection.JavaConverters._

import com.typesafe.config._

import org.apache.spark.sql.SparkSession

import ai.tripl.arc.util.log.logger.Logger
import ai.tripl.arc.api.API.ARCContext
import ai.tripl.arc.util.EitherUtils._
import ai.tripl.arc.config.Error._
import ai.tripl.arc.plugins.DynamicConfigurationPlugin

class DeltaPeriodDynamicConfigurationPlugin extends DynamicConfigurationPlugin {

  val version = ai.tripl.arc.plugins.config.deltaperiod.BuildInfo.version

  def instantiate(index: Int, config: Config)(implicit spark: SparkSession, logger: ai.tripl.arc.util.log.logger.Logger, arcContext: ARCContext): Either[List[StageError], Config] = {
    import ai.tripl.arc.config.ConfigReader._
    import ai.tripl.arc.config.ConfigUtils._
    implicit val c = config

    val expectedKeys = "type" :: "environments" :: "returnName" :: "lagDays" :: "leadDays" :: "formatter" :: "currentDate" :: Nil
    val returnName = getValue[String]("returnName")
    val lagDays = getValue[Int]("lagDays")
    val leadDays = getValue[Int]("leadDays")
    val formatter = getValue[String]("formatter") |> parseFormatter("formatter") _
    val currentDate = formatter match {
      case Right(formatter) => {
        if (c.hasPath("currentDate")) getValue[String]("currentDate") |> parseCurrentDate("currentDate", formatter) _ else Right(java.time.LocalDate.now)
      }
      case _ => Right(java.time.LocalDate.now)
    }
    val invalidKeys = checkValidKeys(c)(expectedKeys)

    (returnName, lagDays, leadDays, formatter, currentDate, invalidKeys) match {
      case (Right(returnName), Right(lagDays), Right(leadDays), Right(formatter), Right(currentDate), Right(invalidKeys)) => 

        val res = (lagDays * -1 to leadDays).map { v =>
          formatter.format(currentDate.plusDays(v))
        }.mkString(",")

        val values = new java.util.HashMap[String, Object]()
        values.put(returnName, res)

        Right(ConfigFactory.parseMap(values))
      case _ =>
        val allErrors: Errors = List(returnName, lagDays, leadDays, formatter, currentDate, invalidKeys).collect{ case Left(errs) => errs }.flatten
        val err = StageError(index, this.getClass.getName, c.origin.lineNumber, allErrors)
        Left(err :: Nil)
    }
  }   
  
  def parseFormatter(path: String)(formatter: String)(implicit c: Config): Either[Errors, DateTimeFormatter] = {
    def err(lineNumber: Option[Int], msg: String): Either[Errors, DateTimeFormatter] = Left(ConfigError(path, lineNumber, msg) :: Nil)

    try {
      Right(DateTimeFormatter.ofPattern(formatter).withResolverStyle(ResolverStyle.SMART))
    } catch {
      case e: Exception => err(Some(c.getValue(path).origin.lineNumber()), e.getMessage)
    }
  }  

  def parseCurrentDate(path: String, formatter: DateTimeFormatter)(value: String)(implicit c: Config): Either[Errors, LocalDate] = {
    def err(lineNumber: Option[Int], msg: String): Either[Errors, LocalDate] = Left(ConfigError(path, lineNumber, msg) :: Nil)

    try {
      Right(LocalDate.parse(value, formatter))
    } catch {
      case e: Exception => err(Some(c.getValue(path).origin.lineNumber()), e.getMessage)
    }
  }    
}
