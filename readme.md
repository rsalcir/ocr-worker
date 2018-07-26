#Ocr-Worker

#Instalação
``` sh
docker pull johnnypark/kafka-zookeeper
docker pull rafaelteixeira/teixeiract
docker pull rsalcir/ocr-worker
```

#Execução
* iniciando o serviço do kafka
``` sh
docker run -p 2181:2181 -p 9092:9092 -e ADVERTISED_HOST=127.0.0.1 johnnypark/kafka-zookeeper
```
* iniciando o serviço de ocr
``` sh
docker run -p 3000:3000 rafaelteixeira/teixeiract
```
* iniciando o worker

-Definindo uma ou mais variaveis de ambiente:
``` sh
docker run --network="host" -e FILA_DE_DOCUMENTOS_NAO_PROCESSADOS=nomeDaFilaDeNaoProcessados -e FILA_DE_DOCUMENTOS_PROCESSADOS=nomeDaFilaDeProcessado -e ... rsalcir/ocr-worker
```
-Utilizando variaveis de ambiente padrão:
``` sh
docker run --network="host" rsalcir/ocr-worker
```
#Formato da mensagem para fila de entrada
``` sh
{"id" : "123", "url" : "https://image.slidesharecdn.com/portugus2b-170225215804/95/texto-verbal-e-noverbal-8-638.jpg"}
```
#Formato da mensagem para fila sucesso
``` sh
{"id" : "123", "texto" : "blablablablablablablablablablablablablablablablablabla..."}
```
#Formato da mensagem para fila erro
``` sh
{"id" : "123", "url" : "https://image.slidesharecdn.com/portugus2b-170225215804/95/texto-verbal-e-noverbal-8-638.jpg"}
```
#Construir o container local
``` sh
sbt docker
```
#Docker Utils
* Delete todos os containers
``` sh
docker rm $(docker ps -a -q)
```
* Delete todas as images
``` sh
docker rmi $(docker images -q)
 ```