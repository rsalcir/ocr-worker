import java.util.concurrent._
import java.util.{Collections, Properties}

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import scalaj.http.Http

import scala.collection.JavaConversions._

object Worker extends App {
  val topic = "documentosTeste"
  val brokers = "localhost:9092"
  val groupId = "group1"

  val properties = createConsumerConfig(brokers, groupId)
  val consumer = new KafkaConsumer[String, String](properties)
  var executor: ExecutorService = null

  def shutdown() = {
    if (consumer != null)
      consumer.close();
    if (executor != null)
      executor.shutdown();
  }

  def createConsumerConfig(brokers: String, groupId: String): Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props
  }

  def run() = {
    System.out.println("***************************INICIO_DA_LEITURA*******************************")
    consumer.subscribe(Collections.singletonList(this.topic))
    Executors.newSingleThreadExecutor.execute(new Runnable {
      override def run(): Unit = {
        while (true) {
          val records = consumer.poll(1000)
          for (record <- records) {
            System.out.println(s"inicio -  ${System.currentTimeMillis()}")
            System.out.println(s"URL:${record.value()}")
            var textoProcessado = Http("http://localhost:8085/ocr")
              .postData(record.value())
              .header("Content-Type", "application/json")
              .header("Charset", "UTF-8")
              .asString.body
            System.out.println(textoProcessado)
            System.out.println(s"fim - ${System.currentTimeMillis()}")
            System.out.println("---------------------------------------")
          }
        }
      }
    })
  }

  run()
}
