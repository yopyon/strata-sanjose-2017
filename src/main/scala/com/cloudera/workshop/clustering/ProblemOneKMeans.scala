package com.cloudera.workshop
import org.apache.log4j._
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.feature.{MinMaxScaler, StandardScaler, VectorAssembler}
import org.apache.spark.sql.functions._

object ProblemOneKMeans{
  Logger.getRootLogger.setLevel(Level.OFF)
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  def main(args: Array[String]) {

    val session = org.apache.spark.sql.SparkSession.builder().
      master("local[4]")
      .appName("ProblemOneKMeans")
      .getOrCreate()

    // Create the DataFrame using csv method
    val dataset = "data/kmeans/flightinfo/flights_nofeatures.csv"
    val inputData = session.read
        .option("header","true")
        .option("inferSchema","true").csv(dataset)

    inputData.printSchema()
    inputData.show(50)

    // Transform Day into Something Usable
    // We are expanding each day into a new feature
    // 1 is the value if its that day, 0 if any other day
    val isSat = udf {(x:String) => if (x.toLowerCase.equals("saturday")) 1 else 0}
    val isSun = udf {(x: String) => if (x.toLowerCase.equals("sunday")) 1 else 0}
    val isMon = udf {(x: String) => if (x.toLowerCase.equals("monday")) 1 else 0}

    val transformedDay = inputData.withColumn("Saturday", isSat(inputData("Day")))
                               .withColumn("Sunday", isSun(inputData("Day")))
                               .withColumn("Monday", isMon(inputData("Day")))
    transformedDay.printSchema()
    transformedDay.show()

    // Transform Time into something usable
    // We are taking time as a fraction of the day
    // That gives us a very good feature to cluster on
    val dayFract = udf {(x:String) =>
                          if (x == null)
                            0
                          else
                          {
                            val formatter = new java.text.SimpleDateFormat("h:m a")
                            val curr = formatter.parse(x).getTime.toDouble
                            val full = formatter.parse("11:59 PM").getTime.toDouble
                            curr/full
                          }
                        }

    // UDF to convert to Int
    val  toInt = udf {(s: String) =>
      s.toInt
    }

    val transformedTime = transformedDay.withColumn ("dateFract",dayFract(transformedDay("Arrival Time")))
                                          .withColumn("Grade",toInt(transformedDay("PayGrade")))
    transformedTime.printSchema()
    transformedTime.show()

    // Use VectorAssembler to assemble feature vector
    // From relevant columns
    val assembler = new VectorAssembler()
                          .setInputCols(Array("Saturday","Sunday","Monday","dateFract","Grade"))
                          .setOutputCol("features")

    val featurizedData = assembler.transform(transformedTime)
    featurizedData.printSchema()
    featurizedData.show(20,false)


    // Scale my features
    val scaler = new MinMaxScaler()
                         .setInputCol("features")
                           .setOutputCol("scaled_features")
    val scalerModel = scaler.fit(featurizedData)
    val scaledData = scalerModel.transform(featurizedData)
    scaledData.printSchema()
    scaledData.show(20,false)

    // Trains a k-means model
    val kmeans = new KMeans()
      .setK(20)
      .setFeaturesCol("scaled_features")
      .setPredictionCol("clusterId")
    val model = kmeans.fit(scaledData)


    val predictedCluster = model.transform(scaledData)
    predictedCluster.printSchema()
    predictedCluster.show(100)


    session.stop()
  }
}
