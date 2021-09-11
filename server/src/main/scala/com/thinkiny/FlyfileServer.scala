package com.thinkiny

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.modules.FileResolverModule

object FlyFileServerMain extends FlyFileServer

class FlyFileServer extends HttpServer {
  override val defaultHttpPort: String = ":8080"
  override val streamRequest: Boolean = true

  override def configureHttp(router: HttpRouter): Unit = {
    FileResolverModule
    router
      .filter[CommonFilters]
      .add[FlyfileController]
  }
}
