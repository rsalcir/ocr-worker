import org.scalatest.{BeforeAndAfter, FunSuite}
import play.api.libs.json.{JsValue, Json}

class WorkerTest extends FunSuite with BeforeAndAfter {

  test("deve_montar_a_mensagem_de_sucesso_no_ocr") {
    var mensagemEsperada: String = s"""{"id" : "001", "texto" : blablablablabla}"""
    val mensagemDaFila: JsValue = Json.parse(s"""{"id" : "001", "url" : "https://minhaImagem.jpg"}""")
    var textoProcessado: String = "blablablablabla";

    var mensagemDeSucesso: String = Worker.montarMensagemDeSucessoNoOcr(mensagemDaFila, textoProcessado);

    assert(mensagemDeSucesso.equals(mensagemEsperada))
  }

  test("deve_montar_a_mensagem_de_erro_no_ocr") {
    var mensagemEsperada: String = s"""{"id" : "002", "url" : "https://minhaImagem.jpg"}"""
    val mensagemDaFila: JsValue = Json.parse(s"""{"id" : "002", "url" : "https://minhaImagem.jpg"}""")

    var mensagemDeErro: String = Worker.montarMensagemDeErroNoOcr(mensagemDaFila);

    assert(mensagemDeErro.equals(mensagemEsperada))
  }
}
