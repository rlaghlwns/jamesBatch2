package com.mangonw.server.mail.sender

import com.mangonw.server.mail.WebMailVO

object MailUtil : MailSender() {
    private var senderEmail: String? = null
    private var senderNm: String? = null
    private var receiverEmail: String? = null
    private var referenceEmail: String? = null
    private var hiddenReferenceEmail: String? = null
    private var subject: String? = null
    private var contents: String? = null

    fun setFieldByWebMailVO(vo: WebMailVO): MailUtil {
        senderEmail = vo.senderEmail
        senderNm = vo.senderNm
        receiverEmail = vo.receiverEmail
        referenceEmail = vo.referenceEmail
        hiddenReferenceEmail = vo.hiddenReferenceEmail
        subject = vo.subject
        contents = vo.content
        return this
    }

    fun subject(arg: String): MailUtil {
        this.subject = arg
        return this
    }

    fun contents(arg: String): MailUtil {
        this.contents = arg
        return this
    }

    fun senderEmail(arg: String): MailUtil {
        this.senderEmail = arg
        return this
    }

    fun senderNm(arg: String): MailUtil {
        this.senderNm = arg
        return this
    }

    fun receiverEmail(arg: String): MailUtil {
        this.receiverEmail = arg
        return this
    }

    fun referenceEmail(arg: String): MailUtil {
        this.referenceEmail = arg
        return this
    }

    fun hiddenReferenceEmail(arg: String): MailUtil {
        this.hiddenReferenceEmail = arg
        return this
    }

    fun send() {
        super.send(getAsWebMailVO())
        init()
    }

    private fun getAsWebMailVO(): WebMailVO {
        val vo = WebMailVO()
        senderEmail?.let { vo.senderEmail = it }
        senderNm?.let { vo.senderNm = it }
        receiverEmail?.let { vo.receiverEmail = it }
        referenceEmail?.let { vo.referenceEmail = it }
        hiddenReferenceEmail?.let { vo.hiddenReferenceEmail = it }
        subject?.let { vo.subject = it }
        contents?.let { vo.content = it }
        return vo
    }

    private fun init() {
        senderEmail = null
        senderNm = null
        receiverEmail = null
        referenceEmail = null
        hiddenReferenceEmail = null
        subject = null
        contents = null
    }

    override fun toString(): String {
        return  "subject : $subject\n" +
                "contents : $contents\n" +
                "senderEmail : $senderEmail\n" +
                "senderNm : $senderNm\n" +
                "receiverEmail : $receiverEmail\n" +
                "referenceEmail : $referenceEmail\n" +
                "hiddenReferenceEmail : $hiddenReferenceEmail\n"
    }
}