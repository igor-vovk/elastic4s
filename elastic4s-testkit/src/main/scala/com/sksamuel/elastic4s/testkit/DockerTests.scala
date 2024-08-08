package com.sksamuel.elastic4s.testkit

import com.dimafeng.testcontainers.{ContainerDef, GenericContainer}
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl, ElasticProperties}
import org.scalatest.Suite

import scala.util.Try

object DockerTests {

  val ContainerImage  = "docker.elastic.co/elasticsearch/elasticsearch"
  val Tag             = "8.13.4"
  val DockerImageName = s"$ContainerImage:$Tag"

  val ElasticContainerDef: ContainerDef = GenericContainer.Def(
    dockerImage = DockerImageName,
    exposedPorts = Seq(9200, 9300),
    env = Map(
      "discovery.type" -> "single-node",
      "network.host" -> "0.0.0.0",
      "path.repo" -> "/tmp/elastic4s",
      "xpack.security.enabled" -> "true",
    ),
    tmpFsMapping = Map(
      "elastic4s-tests/src/test/resources/elasticsearch.yml" -> "/usr/share/elasticsearch/config/elasticsearch.yml"
    ),
  )

}

trait DockerTests extends ElasticDsl with ClientProvider with TestContainerForAll {
  self: Suite =>

  override val containerDef: ContainerDef = DockerTests.ElasticContainerDef

  val elasticHost = sys.env.getOrElse("ES_HOST", "127.0.0.1")
  val elasticPort = sys.env.getOrElse("ES_PORT", "9200")
  val client = ElasticClient(JavaClient(ElasticProperties(s"http://$elasticHost:$elasticPort")))

  protected def deleteIdx(indexName: String): Unit = {
    Try {
      client.execute {
        ElasticDsl.deleteIndex(indexName)
      }.await
    }
  }

  protected def createIdx(name: String) = Try {
    client.execute {
      createIndex(name)
    }.await
  }

  protected def createIdx(name: String, shards: Int) = Try {
    client.execute {
      createIndex(name).shards(shards)
    }.await
  }

  protected def cleanIndex(indexName: String): Unit = {
    deleteIdx(indexName)
    createIdx(indexName)
  }
}
