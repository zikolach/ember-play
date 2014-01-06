package service

import play.api.Play._
import scala.Some
import scala.language.implicitConversions

object Mail {
  lazy val accountName = current.configuration.getString("mail.account.name").getOrElse("")
  lazy val accountPassword = current.configuration.getString("mail.account.password").getOrElse("")
  lazy val serverName = current.configuration.getString("mail.server.name").getOrElse("")
  lazy val serverPort = current.configuration.getInt("mail.server.port").getOrElse(0)


  implicit def stringToSeq(single: String): Seq[String] = Seq(single)
  implicit def liftToOption[T](t: T): Option[T] = Some(t)

  sealed abstract class MailType
  case object Plain extends MailType
  case object Rich extends MailType
  case object MultiPart extends MailType

  case class Mail(
                   from: (String, String), // (email -> name)
                   to: Seq[String],
                   cc: Seq[String] = Seq.empty,
                   bcc: Seq[String] = Seq.empty,
                   subject: String,
                   message: String,
                   richMessage: Option[String] = None,
                   attachment: Option[(java.io.File)] = None
                   )

  object send {
    def a(mail: Mail) {
      import org.apache.commons.mail._

      val format =
        if (mail.attachment.isDefined) MultiPart
        else if (mail.richMessage.isDefined) Rich
        else Plain

      val commonsMail: Email = format match {
        case Plain => new SimpleEmail().setMsg(mail.message)
        case Rich => new HtmlEmail().setHtmlMsg(mail.richMessage.get).setTextMsg(mail.message)
        case MultiPart => {
          val attachment = new EmailAttachment()
          attachment.setPath(mail.attachment.get.getAbsolutePath)
          attachment.setDisposition(EmailAttachment.ATTACHMENT)
          attachment.setName(mail.attachment.get.getName)
          new MultiPartEmail().attach(attachment).setMsg(mail.message)
        }
      }

      // TODO Set authentication from your configuration, sys properties or w/e


      commonsMail.setHostName(serverName)
      commonsMail.setSmtpPort(serverPort)
      commonsMail.setAuthenticator(new DefaultAuthenticator(accountName, accountPassword))
      commonsMail.setSSLOnConnect(true)

      // Can't add these via fluent API because it produces exceptions
      mail.to foreach commonsMail.addTo
      mail.cc foreach commonsMail.addCc
      mail.bcc foreach commonsMail.addBcc

      commonsMail.
        setFrom(mail.from._1, mail.from._2).
        setSubject(mail.subject).
        send()
    }
  }
}
