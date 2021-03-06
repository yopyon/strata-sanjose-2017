package org.cloudera.workshop

import org.apache.spark.ml.feature._
import org.apache.spark.sql.SparkSession

object extractorssol {


  def main(args: Array[String]) {
    val spark = SparkSession
      .builder
      .appName("TfIdfExample")
      .getOrCreate()

    /**
      * Create the Dataframe to be used for
      */

    val sentenceData = spark.createDataFrame(Seq(
      (0.0, "It was a bright cold day in April, and the clocks were striking thirteen."),
      (0.0, "The sky above the port was the color of television, tuned to a dead channel."),
      (1.0, "It was love at first sight.")
    )).toDF("label", "sentence")

    /**
      * First solution - TF-IDF
      */

    val tokenizer = new Tokenizer().setInputCol("sentence").setOutputCol("words")
    val wordsData = tokenizer.transform(sentenceData)

    /**
      * Demonstrate this with CountVectorizer as well
      */

    val hashingTF = new HashingTF()
      .setInputCol("words").setOutputCol("rawFeatures").setNumFeatures(20)

    val featurizedData = hashingTF.transform(wordsData)

    val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
    val idfModel = idf.fit(featurizedData)

    val rescaledData = idfModel.transform(featurizedData)
    rescaledData.select("label", "features").show()

    /**
      * Work on CountVectorizer
      */

    val cvModel: CountVectorizerModel = new CountVectorizer()
      .setInputCol("words")
      .setOutputCol("features")
      .setVocabSize(3)
      .setMinDF(2)
      .fit(wordsData)

    // alternatively, define CountVectorizerModel with a-priori vocabulary
    val cvm = new CountVectorizerModel(Array("a", "b", "c"))
      .setInputCol("words")
      .setOutputCol("features")

    cvModel.transform(wordsData).show(false)


  }
}
