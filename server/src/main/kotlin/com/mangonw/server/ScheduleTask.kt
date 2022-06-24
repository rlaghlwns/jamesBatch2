package com.mangonw.server

import com.mangonw.server.home.Service
import com.mangonw.server.mail.MailProcessor
import com.mangonw.server.mail.WebMailVO
import kotlinx.coroutines.*
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerResponse.async
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import kotlin.system.measureTimeMillis

@Slf4j
@Component
class ScheduleTask {

    @Autowired
    private lateinit var mailProcessor: MailProcessor

    private val threadLocal = ThreadLocal<String?>()

    // 0초 0분 0시 매일 매월 매년
    @Scheduled(cron = "0 0 0 * * *")
    fun task0() {
        COUNT = 0
    }

    @Autowired
    private lateinit var service: Service

    @Scheduled(fixedDelay = 1000 * 60 * 1)
    fun task2() {
        println("start : ${LocalDateTime.now()}")
        try {// 100개 31초정도 걸림 & 리스트 서치 시간이 10초 정도
            val df1 = SimpleDateFormat("yyyyMMdd")
            val ymd = df1.format(Date())
            val df2 = SimpleDateFormat("HHmmss")
            val hms = df2.format(Date())
            val s = "$ymd-${++COUNT}-$hms"

            service.updateNotProcessedMail(s)
            val processingList: List<WebMailVO> = service.selectNotProcessedMail(s)

            val processingList1 = processingList.filter { it.rn in (1..30) }
            val processingList2 = processingList.filter { it.rn in (31..60) }
            val processingList3 = processingList.filter { it.rn in (61..90) }
            val processingList4 = processingList.filter { it.rn in (91..120) }

            runBlocking {
                val time = measureTimeMillis {
                    val j1 = async { mailProcessor.writeToFile2(s, processingList1) }
                    val j2 = async { mailProcessor.writeToFile2(s, processingList2) }
                    val j3 = async { mailProcessor.writeToFile2(s, processingList3) }
                    val j4 = async { mailProcessor.writeToFile2(s, processingList4) }
                    j1.await()
                    j2.await()
                    j3.await()
                    j4.await()
                }
                println("Completed in $time ms")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        println("end : ${LocalDateTime.now()}")
    }

    /*@Scheduled(fixedDelay = 1000 * 60 * 10)
    fun task1() {
        try {// 100개 31초정도 걸림 & 리스트 서치 시간이 10초 정도

            println("start : ${LocalDateTime.now()}")
            runBlocking {
                val time = measureTimeMillis {
                    val j1 = async { doProcessing() }
                    val j2 = async { doProcessing() }
                    val j3 = async { doProcessing() }
                    val j4 = async { doProcessing() }
                    j1.await()
                    j2.await()
                    j3.await()
                    j4.await()
                }
                println("Completed in $time ms")
            }
            println("end : ${LocalDateTime.now()}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun doProcessing() {
        val df1 = SimpleDateFormat("yyyyMMdd")
        val ymd = df1.format(Date())
        val df2 = SimpleDateFormat("HHmmss")
        val hms = df2.format(Date())
        val s = "$ymd-${++COUNT}-$hms"
        println("$s start : ${LocalDateTime.now()}")
        //mailProcessor.writeToFile(s)
        delay(5000)
        println("$s end : ${LocalDateTime.now()}")
    }*/

    private companion object {
        private var COUNT = 0
    }
}