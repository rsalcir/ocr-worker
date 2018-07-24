#Ocr-Worker

#Instalação

* docker pull johnnypark/kafka-zookeeper

* docker pull rafaelteixeira/teixeiract

* docker pull rsalcir/ocr-worker
 
#Execução

* inicie o serviço do kafka
 => docker run -p 2181:2181 -p 9092:9092 -e ADVERTISED_HOST=127.0.0.1 johnnypark/kafka-zookeeper
 
* inicie o serviço de ocr
 => docker run -p 3000:3000 rafaelteixeira/teixeiract
 
* inicie o worker
 => docker run --network="host" rsalcir/ocr-worker http://localhost:3000 localhost:9092 arquivosNaoProcessados arquivosProcessados arquivosComErro
 
#Formato da mensagem para fila de entrada
 
{"id" : "123", "url" : "https://image.slidesharecdn.com/portugus2b-170225215804/95/texto-verbal-e-noverbal-8-638.jpg"}

#Formato da mensagem para fila sucesso

{"id" : "123", "texto" : "blablablablablablablablablablablablablablablablablabla..."}

#Formato da mensagem para fila erro

{"id" : "123", "url" : "https://image.slidesharecdn.com/portugus2b-170225215804/95/texto-verbal-e-noverbal-8-638.jpg"}
  
#Construir o container local

* execute o comando sbt docker

#Docker Utils

* Delete todos os containers
 => docker rm $(docker ps -a -q)

* Delete todas as images
 => docker rmi $(docker images -q)