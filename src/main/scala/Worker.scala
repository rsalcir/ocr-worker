import java.util.concurrent._
import java.util.{Collections, Properties}

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scalaj.http.Http

import scala.collection.JavaConversions._

object Worker extends App {
  val FILA_DE_DOCUMENTOS_NAO_PROCESSADOS = "arquivosNaoProcessados"
  val FILA_DE_DOCUMENTOS_PROCESSADOS = "arquivosProcessados"
  val BROKERS = "localhost:9092"
  val GROUP_ID = "group1"

  val consumerProperties = createConsumerConfig(BROKERS, GROUP_ID)
  val consumer = new KafkaConsumer[String, String](consumerProperties)

  val producerProperties =createProducerConfig(BROKERS)
  var executor: ExecutorService = null

  def createConsumerConfig(brokers: String, groupId: String): Properties = {
    val properties = new Properties()
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
    properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    properties
  }

  def createProducerConfig(brokers: String): Properties = {
    val properties = new Properties()
    properties.put("bootstrap.servers", brokers)
    properties.put("client.id", "OCRProducer")
    properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties
  }

  def shutdown() = {
    if (consumer != null)
      consumer.close();
    if (executor != null)
      executor.shutdown();
  }

  def run() = {
    System.out.println("***************************INICIO_DA_LEITURA*******************************")
    consumer.subscribe(Collections.singletonList(this.FILA_DE_DOCUMENTOS_NAO_PROCESSADOS))
    Executors.newSingleThreadExecutor.execute(new Runnable {
      override def run(): Unit = {
        while (true) {
          val records = consumer.poll(1000)
          for (record <- records) {
            System.out.println(s"inicio -  ${System.currentTimeMillis()}")
            System.out.println(s"URL:${record.value()}")
            var textoProcessado = Http("http://localhost:3000").param("image", record.value()).asString.body
            val producer = new KafkaProducer[String, String](producerProperties)
            val texto = new ProducerRecord[String, String](FILA_DE_DOCUMENTOS_PROCESSADOS, textoProcessado)
            producer.send(texto)
            producer.close()
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
