package com.mangonw.server.home

import com.mangonw.server.mail.WebMailVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class Service {

    @Autowired
    private lateinit var dao: Dao

    fun insertMailAtcFile(insert: WebMailVO) = dao.insertMailAtcFile(insert)
    fun selectNotProcessedMail(s: String) = dao.selectNotProcessedMail(s)
    fun updateNotProcessedMail(s: String) = dao.updateNotProcessedMail(s)
    fun selectBoxSetting(result: WebMailVO) = dao.selectBoxSetting(result)
    fun mailDataError(result: WebMailVO) = dao.mailDataError(result)
    fun mailDataProcessing(result: WebMailVO) = dao.mailDataProcessing(result)
    fun mailProUpdate(result: WebMailVO) = dao.mailProUpdate(result)
    fun absenceUser(result: WebMailVO) = dao.absenceUser(result)
    fun selectFile(searchVo: WebMailVO) = dao.selectFile(searchVo)
    fun test() {
        dao.test()
    }

}