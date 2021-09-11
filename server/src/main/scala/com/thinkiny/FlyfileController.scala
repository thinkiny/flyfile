package com.thinkiny

import com.twitter.concurrent.AsyncStream
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.streaming.StreamingRequest
import com.twitter.io.Buf
import com.twitter.util.Future
import net.lingala.zip4j.ZipFile

import java.io.BufferedOutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.sys.process._

class FlyfileController extends Controller {
  lazy val homeDir = System.getProperty("user.home")
  val tmpFilePath = Paths.get("/tmp/flyfile.zip")

  def createOutput(path: Path): BufferedOutputStream = {
    val parent = path.getParent()
    if (!Files.exists(parent)) {
      Files.createDirectories(parent)
    }

    Files.createFile(path)
    new BufferedOutputStream(
      Files.newOutputStream(
        path,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      )
    )
  }

  def unzipFile1(src: Path, dst: Path): Unit = {
    val zip = new ZipFile(src.toFile())
    zip.setCharset(Charset.forName("UTF-8"))
    zip.extractAll(dst.toString())
  }

  def unzipFile(src: Path, dst: Path): Unit = {
    s"7z -y -r x ${src} -o${dst}".!
  }

  def updatePermsFile(path: Path, mode: Option[String]): Unit = {
    mode.foreach { m =>
      val perms = PosixFilePermissions.fromString(m)
      Files.setPosixFilePermissions(path, perms)
    }
  }

  put("/upload") { req: StreamingRequest[AsyncStream, Buf] =>
    val path = req.request.getParam("path")
    val unzip = req.request.getBooleanParam("unzip", false)
    val mode = Option(req.request.getParam("mode")).filter(!_.isEmpty())
    if (path == null) {
      Future("missing path param")
    } else {
      val uploadPath =
        if (path.startsWith("/")) Paths.get(path)
        else Paths.get(homeDir + "/" + path)

      Try(createOutput(tmpFilePath)) match {
        case Success(output) => {
          req.stream
            .foldLeft(MessageDigest.getInstance("MD5")) { (md5, buf) =>
              val bs = Buf.ByteArray.Owned.extract(buf)
              md5.update(bs)
              output.write(bs)
              md5
            }
            .map(md5 => md5.digest().map("%02x".format(_)).mkString + "\n")
            .onSuccess { _ =>
              output.flush()
              if (unzip) {
                unzipFile(tmpFilePath, uploadPath.getParent())
                Files.delete(tmpFilePath)
              } else {
                Files.move(tmpFilePath, uploadPath)
                updatePermsFile(uploadPath, mode)
              }
            }
            .ensure(output.close())
        }
        case Failure(e) => {
          Future(e.getMessage())
        }
      }
    }
  }
}
