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
 
#Construir o container local

* execute o comando sbt docker

#Docker Utils

* Delete todos os containers
 => docker rm $(docker ps -a -q)

* Delete todas as images
 => docker rmi $(docker images -q)