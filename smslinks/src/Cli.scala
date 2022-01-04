import java.nio.file.{Path => JPath}

import zio.{App, URIO, ZIO}
import zio.blocking.Blocking
import zio.console.{putStrLn, Console}

import zio.cli._
import zio.cli.HelpDoc.Span.text

object Cli extends App {

  sealed trait Subcommand extends Product with Serializable
  object Subcommand {
    final case class Load(phone: String, directory: JPath) extends Subcommand
  }

  val sourcePhone: Options[String] = Options.text("phone")

  val loadHelp: HelpDoc = HelpDoc.p("Load links from xml file backup")
  val load =
    Command("xml", sourcePhone, Args.file("xml-file"))
      .withHelp(loadHelp)
      .map { case (phone, file) => Subcommand.Load(phone, file) }

  val wc: Command[Subcommand] =
    Command("load-tool", Options.none, Args.none).subcommands(load)

  def exec(cmd: Subcommand) = cmd match {
    case Subcommand.Load(phone, path) => XMLLoader.links(path, phone)
  }

  val wcApp = CliApp.make(
    "Load Tool",
    "1.0.0",
    text("Extract links from SMS backup"),
    wc
  )(exec)

  override def run(args: List[String]) = wcApp.run(args)
}
