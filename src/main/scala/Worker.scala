import java.util.concurrent._
import java.util.{Collections, Properties}
import java.text.SimpleDateFormat;  
import java.util.Date;  

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecords}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import play.api.libs.json.{JsValue, _}
import scalaj.http.Http

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

import scala.collection.JavaConversions._

object Worker extends App {

  val SERVICO_OCR = sys.env.getOrElse("HOST_SERVICO_OCR", "http://localhost:3000")
  val HOST = sys.env.getOrElse("HOST_KAFKA", "localhost:9092")
  val FILA_DE_DOCUMENTOS_NAO_PROCESSADOS = sys.env.getOrElse("FILA_DE_DOCUMENTOS_NAO_PROCESSADOS", "ArquivosNaoProcessados_DEV")
  val FILA_DE_DOCUMENTOS_PROCESSADOS = sys.env.getOrElse("FILA_DE_DOCUMENTOS_PROCESSADOS", "ArquivosProcessados_DEV") 
  val FILA_DE_ERRO_NO_PROCESSAMENTO_DOS_DOCUMENTOS = sys.env.getOrElse("FILA_DE_ERRO_NO_PROCESSAMENTO_DOS_DOCUMENTOS", "ArquivosComErro_DEV")

  val CLIENTE_PARA_SUCESSO = "OcrProdutorDaFilaDeSucesso"
  val CLIENTE_PARA_ERRO = "OcrProdutorDaFilaDeErro" 

  val propriedadesDoConsumidorDaFila = montarConfiguracoesDoConsumidorDaFila()
  val consumidorDaFila = new KafkaConsumer[String, String](propriedadesDoConsumidorDaFila)
  var executor: ExecutorService = null

  def montarConfiguracoesDoConsumidorDaFila(): Properties = {
    val properties = new Properties()
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, HOST)
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "group1")
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
    properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    properties
  }

  def montarConfiguracoesDoProdutorDaFila(identificadorDoCliente: String): Properties = {
    val properties = new Properties()
    properties.put("bootstrap.servers", HOST)
    properties.put("client.id", identificadorDoCliente)
    properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties
  }

  def shutdown() = {
    if (consumidorDaFila != null)
      consumidorDaFila.close();
    if (executor != null)
      executor.shutdown();
  }

  def executar() = {
    exibirVariaveisDeAmbienteConfiguradas()
    inicializarLogs()
    consumidorDaFila.subscribe(Collections.singletonList(this.FILA_DE_DOCUMENTOS_NAO_PROCESSADOS))
    Executors.newSingleThreadExecutor.execute(new Runnable {
      override def run(): Unit = {
        while (true) {
          val mensagensDaFila = consumidorDaFila.poll(1000)
          processarMensagensDaFila(mensagensDaFila);
        }
      }
    })
  }

  def inicializarLogs() = {
    System.out.println("-> worker em execução!")
    
    val propriedadesDoProdutor = montarConfiguracoesDoProdutorDaFila(CLIENTE_PARA_SUCESSO)
    val formatacaoDaData = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  
    val mensagem = "Work do OCR iniciado as " + formatacaoDaData.format(new Date())
    val topico = "ExecucaoDosWorkes"
    enviarMensagemParaFila(mensagem, topico, propriedadesDoProdutor)

    testarApiOcr().onComplete {
      case Success(textoProcessado) => {}
      case Failure(erro) => {
        System.out.println("-> Erro ao acessar API")
      }
    }
  }

  def processarMensagensDaFila(mensagensDaFila: ConsumerRecords[String, String]) = {
    for (mensagemDaFila <- mensagensDaFila) {
      if(mensagemDaFila.value() != null && !mensagemDaFila.value().isEmpty()) {
        val mensagemEmJson = converterMensagemParaJson(mensagemDaFila.value())
        if(mensagemEmJson != null)
          executarOcrEProcessarRetorno(mensagemEmJson)
      }
    }
  }

  def converterMensagemParaJson(mensagem: String): JsValue = {
    
    try{
      val json = Json.parse(mensagem)
      return json
    }
    catch {
      case e: Exception => {
        System.out.println("-> Erro ao converter json")
        return null
      }
    }
  }

  def executarOcrEProcessarRetorno(mensagem: JsValue){
    executarOcr(mensagem).onComplete {
      case Success(textoProcessado) => {
        System.out.println("-> Texto processado no OCR: " + mensagem)
        val mensagemParaFila = montarMensagemDeSucessoNoOcr(mensagem, textoProcessado)
        val propriedadesDoProdutorDaFilaDeSucesso = montarConfiguracoesDoProdutorDaFila(CLIENTE_PARA_SUCESSO)
        enviarMensagemParaFila(mensagemParaFila, FILA_DE_DOCUMENTOS_PROCESSADOS, propriedadesDoProdutorDaFilaDeSucesso)
      }
      case Failure(erro) => {
        System.out.println("-> Erro ao processar no OCR: " + mensagem + " | Erro: " + erro)
        val mensagemParaFila = montarMensagemDeErroNoOcr(mensagem)
        val propriedadesDoProdutorDaFilaDeErros = montarConfiguracoesDoProdutorDaFila(CLIENTE_PARA_ERRO)
        enviarMensagemParaFila(mensagemParaFila, FILA_DE_ERRO_NO_PROCESSAMENTO_DOS_DOCUMENTOS, propriedadesDoProdutorDaFilaDeErros)
      }
    }
  }

  def executarOcr(mensagem: JsValue): Future[String] = Future {
    def executar(): Future[String] = Future {
      val url = (mensagem \ "url").as[String]
      Http(SERVICO_OCR).param("image", url).asString.body
    }

    Await.result(executar(), 100 second)
  }

  def exibirVariaveisDeAmbienteConfiguradas() = {
    System.out.println("-> variaveis de ambiente configuradas")
    val variaveisDeAmbiente = Map("HOST_SERVICO_OCR" -> SERVICO_OCR,
      "HOST_KAFKA" -> HOST,
      "FILA_DE_DOCUMENTOS_NAO_PROCESSADOS" -> FILA_DE_DOCUMENTOS_NAO_PROCESSADOS,
      "FILA_DE_DOCUMENTOS_PROCESSADOS" -> FILA_DE_DOCUMENTOS_PROCESSADOS,
      "FILA_DE_ERRO_NO_PROCESSAMENTO_DOS_DOCUMENTOS" -> FILA_DE_ERRO_NO_PROCESSAMENTO_DOS_DOCUMENTOS)
    for ((variavel, valorDefinido) <- variaveisDeAmbiente) println(s"-ENV $variavel=$valorDefinido")
  }

  def testarApiOcr(): Future[String] = Future {
    def executar(): Future[String] = Future {
      Http(SERVICO_OCR).asString.body
    }

    Await.result(executar(), 30 second)
  }

  def montarMensagemDeSucessoNoOcr(mensagem: JsValue, textoProcessado: String): String = {
    val identificador = (mensagem \ "id").as[String]
    s"""{"id" : "${identificador}", "texto" : ${textoProcessado}}"""
  }

  def montarMensagemDeErroNoOcr(mensagem: JsValue): String = {
    val identificador = (mensagem \ "id").as[String]
    val url = (mensagem \ "url").as[String]
    s"""{"id" : "${identificador}", "url" : "${url}"}"""
  }

  def enviarMensagemParaFila(mensagem: String, fila: String, propriedadesDaFila: Properties) = {
    try{
      Thread.currentThread().setContextClassLoader(null);
      val produtorDaFila = new KafkaProducer[String, String](propriedadesDaFila)
      val mensagemParaFila = new ProducerRecord[String, String](fila, mensagem)
      produtorDaFila.send(mensagemParaFila)
      produtorDaFila.close()
    }catch{
      case e: Exception => {
        System.out.println("-> Erro ao acessar Kafka: " + e.getMessage)
      }
    }
  }

  executar()
}
