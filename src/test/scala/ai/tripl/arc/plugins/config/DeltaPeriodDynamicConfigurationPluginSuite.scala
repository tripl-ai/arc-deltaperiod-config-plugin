package ai.tripl.arc.plugins

import ai.tripl.arc.ARC
import ai.tripl.arc.api.API._
import ai.tripl.arc.config.ArcPipeline
import ai.tripl.arc.config.Error._
import ai.tripl.arc.util.TestUtils

import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfter, FunSuite}
import ai.tripl.arc.extract.ParquetExtract

class DeltaPeriodDynamicConfigurationPluginSuite extends FunSuite with BeforeAndAfter {

  var session: SparkSession = _  

  before {
    implicit val spark = SparkSession
                  .builder()
                  .master("local[*]")
                  .config("spark.ui.port", "9999")
                  .appName("Spark ETL Test")
                  .getOrCreate()
    spark.sparkContext.setLogLevel("INFO")
    implicit val logger = TestUtils.getLogger()

    // set for deterministic timezone
    spark.conf.set("spark.sql.session.timeZone", "UTC")   

    session = spark
    import spark.implicits._
  }

  after {
    session.stop()
  }

  test("DeltaPeriodDynamicConfigurationPluginSuite") {
    implicit val spark = session
    import spark.implicits._
    implicit val logger = TestUtils.getLogger()
    implicit val arcContext = TestUtils.getARCContext(isStreaming=false)

    val conf = s"""{
      "plugins": {
        "config": [
          {
            "type": "DeltaPeriodDynamicConfigurationPlugin",
            "environments": ["test"],
            "returnName": "ETL_CONF_DELTA_PERIOD",
            "lagDays": 10,
            "leadDays": 1,
            "formatter": "uuuu-MM-dd"
          }      
        ]
      },
      "stages": [
        {
          "type": "ParquetExtract",
          "name": "test",
          "description": "test",
          "environments": [
            "production",
            "test"
          ],
          "inputURI": "/tmp/customer/{"$${ETL_CONF_DELTA_PERIOD}"}/*.parquet",
          "outputView": "test"
        }
      ]
    }"""
    
    val pipelineEither = ArcPipeline.parseConfig(Left(conf), arcContext)

    // assert graph created
    pipelineEither match {
      case Left(err) => {
        println(err)
        assert(false)
      }
      case Right((pipeline, _)) => {
        pipeline.stages(0) match {
          case s: ai.tripl.arc.extract.ParquetExtractStage => assert(s.input.count(_ == ',') == 11)
          case _ => assert(false)
        }
      }
    }
  }
  
}
