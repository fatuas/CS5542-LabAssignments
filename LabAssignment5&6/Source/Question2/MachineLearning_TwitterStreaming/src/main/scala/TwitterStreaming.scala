import java.io.{PrintWriter, File}

import edu.umkc.fv.NLPUtils._
import edu.umkc.fv.Utils._
import org.apache.spark.SparkConf
import org.apache.spark.mllib.classification.{NaiveBayesModel, NaiveBayes}
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
   * Modified by sindhu on March 2 2015
 */
object TwitterStreaming {

  def main(args: Array[String]) {

    val trainingKeywords = Array("Box", "Chair", "Door", "Lamp", "Stand")

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generate OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", "hkce7PkPYfBvHjrLGh8Um4FD1")
    System.setProperty("twitter4j.oauth.consumerSecret", "PGix2mcFegGb0pPh2HT7UJNkzmQlfT8MNEcqDKwXP99gPmWVUZ")
    System.setProperty("twitter4j.oauth.accessToken", "3535791433-5Xm2uZXXODJNkjc2BO3gOGuQDVEuOWccDOfKfAx")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "LharOF5IVt00dkA5DFCq1IME2xvBsyE9gPPuX3MujCXnm")

    //Create a spark configuration with a custom name and master
    // For more master configuration see  https://spark.apache.org/docs/1.2.0/submitting-applications.html#master-urls
    val sparkConf = new SparkConf().setAppName("TweetsApp").setMaster("local[*]")
    //Create a Streaming Context with 2 second window
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    val sc = ssc.sparkContext


    //Loop from here to ENDTRAINING, with different keywords each time
    for(keyword <- trainingKeywords) {
      println(" " + keyword)
      val filters = Array(keyword)
      //Using the streaming context, open a twitter stream (By the way you can also use filters)
      //Stream generates a series of random tweets
      val tweets = TwitterUtils.createStream(ssc, None, filters)

      //Get all of the tweet data that matches the filter
      val tweetData = tweets.flatMap(status => status.getText.split(" ")) //.filter(_.startsWith("#")))

      //Now to write all of the messages to a training file
      //val tweetFile = "data/training/" + keyword + ".txt"
      //val pw = new PrintWriter(new File(tweetFile))
      //pw.write(tweetData.toString())
      //pw.close
      tweetData.saveAsTextFiles("data/training/")
    }
    //ENDTRAINING


    var model: NaiveBayesModel = null
    //Now to analyze the training data
    val labelToNumeric = createLabelMap("data/training/")
    val training = sc.wholeTextFiles("data/training/*")
      .map(rawText => createLabeledDocument(rawText, labelToNumeric))
    val X_train = tfidfTransformer(training)
    X_train.foreach(vv => println(vv))
    model = NaiveBayes.train(X_train, lambda = 1.0)


    //Next collect data unsorted by keyword
    println("Collecting unfiltered tweets")
    val filter = Array("directions")
    //Using the streaming context, open a twitter stream (By the way you can also use filters)
    //Stream generates a series of random tweets
    val tweets = TwitterUtils.createStream(ssc, None, filter)
    //Get all of the tweet data that matches the filter
    val tweetData = tweets.flatMap(status => status.getText.split(" ")) //.filter(_.startsWith("#")))
    //Now to write all of the messages to a training file
    //val tweetFile = "data/testing/unknown.txt"
    //val pw = new PrintWriter(new File(tweetFile))
    //pw.print(tweetData)
    //pw.close
    tweetData.saveAsTextFiles("data/testing/")


    println("identifying unfiltered tweets")
    //Last, analyze the raw data
    val lines=sc.wholeTextFiles("data/testing/*")
    val data = lines.map(line => {
      val test = createLabeledDocumentTest(line._2, labelToNumeric)
      println(test.body)
      test
    })
    val X_test = tfidfTransformerTest(sc, data)
    val predictionAndLabel = model.predict(X_test)
    println("PREDICTION")
    predictionAndLabel.foreach(x => {
      labelToNumeric.foreach {
        y => if (y._2 == x) {
          println(y._1)
        }
      }
    })


    //Now kick it all off
    ssc.start()

    //var s:String="Twitter feed complete\n"
    //SocketClient.sendCommandToAndroid(s)
    println("Program complete")

    ssc.awaitTermination()
  }
}
