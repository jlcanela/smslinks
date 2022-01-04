import zio._
import zio.stream._
import zio.blocking.Blocking

import java.io.IOException
import java.io.FileInputStream
import java.nio.file.{Path => JPath}

import javax.xml.stream.events.XMLEvent
import com.ctc.wstx.api.WstxInputProperties
import com.ctc.wstx.stax.WstxInputFactory
import scala.xml.Elem

import xs4s.syntax.zio._
import xs4s.ziocompat._
import xs4s.XmlElementExtractor


// Hack to fix FCPX XML LUT problems with massive attribute data
class InputFactory extends WstxInputFactory {
    setProperty(WstxInputProperties.P_MAX_ATTRIBUTE_SIZE, Integer.MAX_VALUE);
}

object XMLLoader {

  System.getProperties().put("javax.xml.stream.XMLInputFactory", "InputFactory");			
	
  def xmlBytes(path: JPath): ZStream[Blocking, IOException, Byte] = Stream.fromInputStreamEffect( blocking.effectBlockingIO {
         new FileInputStream(path.toString)
     }, 1024)

  def output = Sink.fromFile(java.nio.file.Paths.get("data", "links.txt")).contramapChunks[String](x => x.flatMap(c => Chunk.fromArray(c.getBytes)))

  def links(path: JPath, origin: String) = for {
    links <- extractLinks(xmlBytes(path), origin).map(_ + "\n").runCollect
    _ <- ZStream.fromIterable(links.toSet.toArray.sorted).run(output)
  } yield ()

  def isOrigin(origin: String) = (el: Elem) => el.attributes.get("address").headOption.map(_.toString)==Some(origin)

  def extractLinks(el: Elem) = el.attributes.get("body")
  .headOption.map(_.toString).getOrElse("")
  .split(" ")
  .filter(!_.isEmpty)
  .map(_.trim)
  .filter(_.startsWith("http"))

  def extractLinks[R <: Blocking](
      byteStream: ZStream[R, IOException, Byte], origin: String
  ): ZStream[R, Throwable, String] = {

    val smsElementExtractor: XmlElementExtractor[Elem] =
      XmlElementExtractor.filterElementsByName("sms")

    val xmlEventStream: ZStream[R, Throwable, XMLEvent] =
      byteStream.via(byteStreamToXmlEventStream()(_))

    val anchorElements: ZStream[R, Throwable, Elem] =
      xmlEventStream.via(smsElementExtractor.toZIOPipeThrowError)

    anchorElements
    .filter(isOrigin(origin))
    .flatMap(el => ZStream.fromIterable(extractLinks(el)))
  }

}
