package com.mangonw.server.mail.sender

import com.mangonw.server.mail.WebMailVO
import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException
import org.springframework.beans.factory.annotation.Value
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

open class MailSender {

    companion object {
        private const val SMTP_ADDR = "117.52.20.181"
    }

    fun send(requestVO: WebMailVO): Int {
        val smtpAddr = SMTP_ADDR
        val senderEmail = requestVO.senderEmail
        val senderNm = requestVO.senderNm
        val receiverEmail = requestVO.receiverEmail
        val referenceEmail = requestVO.referenceEmail
        val hiddenReferenceEmail = requestVO.hiddenReferenceEmail
        val subject = requestVO.subject
        val content = requestVO.content
            .replace("\n","<br>")
            .replace("\t","&nbsp;&nbsp;&nbsp;&nbsp;")
        val props = Properties()
        props["mail.smtp.host"] = smtpAddr
        val session: Session = Session.getDefaultInstance(props, null)

        val message = MimeMessage(session)
        try {
            message.setFrom(InternetAddress(senderEmail, senderNm, "UTF-8"))
            message.setHeader("Content-Transfer-Encoding", "base64")
            val tos: Array<InternetAddress> = InternetAddress.parse(receiverEmail)
            message.setRecipients(Message.RecipientType.TO, tos)
            if (referenceEmail != "") {
                val cc: Array<InternetAddress> = InternetAddress.parse(referenceEmail)
                message.setRecipients(Message.RecipientType.CC, cc)
            }
            if (hiddenReferenceEmail != "") {
                val bcc: Array<InternetAddress> = InternetAddress.parse(hiddenReferenceEmail)
                message.setRecipients(Message.RecipientType.BCC, bcc)
            }
            message.subject = subject
            message.sentDate = Date()
            message.setContent(content, "text/html;charset=utf-8")
            val transport: Transport = session.getTransport("smtp")
            Transport.send(message)
            transport.close()
            return 1
        } catch (e: MessagingException) {
            e.printStackTrace()
            return 0
        }
    }
}