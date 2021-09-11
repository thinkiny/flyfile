package com.thinkiny

import com.twitter.finagle.Http
import com.twitter.finagle.http.Method
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Version
import com.twitter.io.Reader
import com.twitter.util.Await
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import net.lingala.zip4j.progress.ProgressMonitor
import scala.annotation.tailrec
import scala.sys.process._
import java.nio.file.Paths

object FlyFileClient {
  val homeDir = System.getProperty("user.home")
  val tmpZipPath = Paths.get("/tmp/flyfile.zip")

  def uploadFile(
      addr: String,
      file: File,
      path: String,
      unzip: Boolean,
      mode: String = ""
  ): Unit = {
    val client = Http.client.withStreaming(true).newService(addr)
    val dstPath =
      if (path.startsWith(homeDir))
        path.substring(homeDir.length() + 1)
      else path

    val request =
      Request(
        Version.Http11,
        Method.Put,
        s"/upload?mode=${mode}&path=${dstPath}&unzip=${unzip}",
        Reader.fromFile(file, 8192)
      )
    println(Await.result(client(request)).contentString)
  }

  @tailrec
  def reportProcess(
      monitor: ProgressMonitor,
      percent: Int = 0,
      round: Int = 1
  ): Unit = {
    if (!monitor.getState().equals(ProgressMonitor.State.READY)) {
      val current = monitor.getPercentDone()
      if (round % 10 == 0 || current != percent) {
        println(s"Percentage done: ${current}");
        println(s"Current task: ${monitor.getCurrentTask()}")
      }
      Thread.sleep(1000)
      reportProcess(monitor, current, round + 1)
    } else {
      println("pack zip done")
    }
  }

  def packFolder(src: String, dst: String): Unit = {
    println(s"start pack ${src}")
    s"7z a -bsp1 ${dst} ${src}".!
  }

  def packFolder1(src: String, dst: String): Unit = {
    val zip = new ZipFile(dst)
    val monitor = zip.getProgressMonitor();

    zip.setCharset(Charset.forName("UTF-8"))
    zip.setRunInThread(true)

    val zipParams = new ZipParameters()
    zipParams.setSymbolicLinkAction(
      ZipParameters.SymbolicLinkAction.INCLUDE_LINK_ONLY
    )
    zip.addFolder(new File(src), zipParams)
    reportProcess(monitor)
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("flyfile $addr $file")
      return
    }

    val uploadPath = args(1).replaceFirst("^~", homeDir)
    val file = new File(uploadPath)
    if (file.isDirectory()) {
      packFolder(uploadPath, tmpZipPath.toString())
      uploadFile(args(0), tmpZipPath.toFile(), uploadPath, true)
      Files.delete(tmpZipPath)
    } else {
      val perms = Files.getPosixFilePermissions(file.toPath())
      val mode = PosixFilePermissions.toString(perms)
      uploadFile(args(0), file, uploadPath, false, mode)
    }
  }
}
