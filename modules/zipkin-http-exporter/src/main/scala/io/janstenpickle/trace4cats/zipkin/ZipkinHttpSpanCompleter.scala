package io.janstenpickle.trace4cats.zipkin

import cats.effect.kernel.{Async, Resource}
import fs2.Chunk
import io.janstenpickle.trace4cats.`export`.{CompleterConfig, QueuedSpanCompleter}
import io.janstenpickle.trace4cats.kernel.SpanCompleter
import io.janstenpickle.trace4cats.model.TraceProcess
import org.http4s.Uri
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ZipkinHttpSpanCompleter {

  def apply[F[_]: Async](
    client: Client[F],
    process: TraceProcess,
    host: String = "localhost",
    port: Int = 9411,
    config: CompleterConfig = CompleterConfig(),
    protocol: String = "http"
  ): Resource[F, SpanCompleter[F]] =
    Resource.eval(Slf4jLogger.create[F]).flatMap { implicit logger: Logger[F] =>
      Resource
        .eval(ZipkinHttpSpanExporter[F, Chunk](client, host, port, protocol))
        .flatMap(QueuedSpanCompleter[F](process, _, config))
    }

  def apply[F[_]: Async](
    client: Client[F],
    process: TraceProcess,
    uri: Uri,
    config: CompleterConfig
  ): Resource[F, SpanCompleter[F]] =
    Resource.eval(Slf4jLogger.create[F]).flatMap { implicit logger: Logger[F] =>
      QueuedSpanCompleter[F](process, ZipkinHttpSpanExporter[F, Chunk](client, uri), config)
    }
}
