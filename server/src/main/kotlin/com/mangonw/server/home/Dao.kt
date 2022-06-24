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

}