package com.mangonw.server.home

import com.mangonw.server.mail.MailboxInfoVO
import com.mangonw.server.mail.WebMailVO
import org.apache.commons.codec.net.QuotedPrintableCodec
import org.apache.ibatis.session.SqlSession
import org.apache.james.mime4j.message.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.*
import javax.mail.Session
import javax.mail.internet.MimeMessage


@Repository
class Dao {

    companion object {
        private const val NAMESPACE = "com.mangonw.server.home.Dao."
    }

    @Autowired
    @Qualifier(value = "sqlSession")
    private lateinit var sqlSession: SqlSession

    fun insertMailAtcFile(insert: WebMailVO): Int {
        return sqlSession.insert(NAMESPACE +"insertMailAtcFile", insert)
    }

    fun updateNotProcessedMail(s: String) {
        sqlSession.update("${NAMESPACE}updateNotProcessedMail", s);
    }

    fun selectNotProcessedMail(s: String): List<WebMailVO> {
        return sqlSession.selectList("${NAMESPACE}selectNotProcessedMail", s);
    }

    fun selectBoxSetting(result: WebMailVO): List<MailboxInfoVO> {
        return sqlSession.selectList("${NAMESPACE}selectBoxSetting", result);
    }

    fun mailDataError(result: WebMailVO): Int {
        return sqlSession.insert("${NAMESPACE}mailDataError", result)
    }

    fun mailDataProcessing(result: WebMailVO): Int {
        return sqlSession.insert("${NAMESPACE}mailDataProcessing", result)
    }

    fun mailProUpdate(result: WebMailVO): Int {
        return sqlSession.update("${NAMESPACE}mailProUpdate", result)
    }

    fun absenceUser(result: WebMailVO): WebMailVO? {
        return sqlSession.selectOne("${NAMESPACE}absenceUser", result)
    }

    fun selectFile(searchVo: WebMailVO): WebMailVO {
        return sqlSession.selectOne("${NAMESPACE}selectFile", searchVo)
    }

    fun test() {
        val v = WebMailVO()
        v.mailboxId = "2"
        v.mailUid = "408"
        val vo: WebMailVO = sqlSession.selectOne("${NAMESPACE}test", v)
        try {
            //  헤더
            val props = System.getProperties()
            val session = Session.getDefaultInstance(props, null)
            val hs = ByteArrayInputStream(vo.headerBytes)
            val hm = MimeMessage(session, hs)

            println(hm.subject)
            println("from")
            hm.from.forEach {
                println(it.toString())
            }

            println("sender")
            println(hm.sender)

            println("allRecipients")
            hm.allRecipients.forEach {
                //println(it.type)  // rfc822
                println(it.toString())
            }

            println(hm.sentDate)
            println(hm.receivedDate)

            //  바디
            val bs = ByteArrayInputStream(vo.mailBytes)
            val msg = Message(bs)

            if(msg.isMultipart) {
                //  본문
                val mp = msg.body as Multipart
                val bps = mp.bodyParts
                (0 until bps.size).forEach {
                    val b = bps[it].body as TextBody
                    b.writeTo(System.out)
                }
                //  첨부
                mp.epilogue.split("--${vo.boundary}").forEach {
                    val ep = "--${vo.boundary}$it"
                    val mm = Message(ByteArrayInputStream(ep.toByteArray()))
                    if(mm.filename != null) {
                        var fn = mm.filename
                        if(
                            mm.filename.startsWith("=?")
                            && mm.filename.endsWith("?=")
                        ) {
                            val fna = fn.split("?")
                            when(fna[2]) {
                                "B" -> {
                                    fn = String(Base64.getDecoder().decode(fna[3]), Charset.forName(fna[1]))
                                }
                                "Q" -> {
                                    fn = QuotedPrintableCodec().decode(fna[3],fna[1])
                                }
                            }
                        }
                        val bb = mm.body as BinaryBody
                        val f = File("/usr/$fn")
                        val fos = FileOutputStream(f)
                        bb.writeTo(fos)
                    }
                }

            }
            else{
                //  본문
                val bd = msg.body as TextBody
                bd.writeTo(System.out)

                //  첨부
                val i = ByteArrayInputStream(vo.mailBytes)
                val bf = BodyFactory()
                val tb = bf.textBody(i)
                val te = tb.reader.readText()
                val arr = te.split("--${vo.boundary}")
                (arr.indices-1).forEach {
                    val ep = "--${vo.boundary}${arr[it]}\r--${vo.boundary}--"
                    val mm = Message(ByteArrayInputStream(ep.toByteArray()))
                    if(mm.filename != null) {
                        var fn = mm.filename
                        if(
                            mm.filename.startsWith("=?")
                            && mm.filename.endsWith("?=")
                        ) {
                            val fna = fn.split("?")
                            when(fna[2]) {
                                "B" -> {
                                    fn = String(Base64.getDecoder().decode(fna[3]), Charset.forName(fna[1]))
                                }
                                "Q" -> {
                                    fn = QuotedPrintableCodec().decode(fna[3],fna[1])
                                }
                            }
                        }
                        val bb = mm.body as BinaryBody
                        val f = File("/usr/$fn")
                        val fos = FileOutputStream(f)
                        bb.writeTo(fos)
                    }
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

}