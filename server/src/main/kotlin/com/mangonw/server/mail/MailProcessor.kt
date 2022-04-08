package com.mangonw.server.mail

import com.mangonw.server.home.Service
import com.mangonw.server.mail.sender.MailUtil
import org.apache.commons.codec.net.QuotedPrintableCodec
import org.apache.james.mime4j.message.Header
import org.apache.james.mime4j.parser.Field
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.internet.MimeUtility
import javax.xml.bind.DatatypeConverter

@Component
class MailProcessor {

	@Value("http://\${server.ip}:\${server.port}")
	private lateinit var rootUrl: String

	@Autowired
	private lateinit var service: Service

	private var step = ""

	suspend fun writeToFile2(s: String, processingList: List<WebMailVO>) {
		//service.updateNotProcessedMail(s)
		//val processingList: List<WebMailVO> = service.selectNotProcessedMail(s)
		for (result in processingList) {
			try {
				step = "데이터 가공 스케쥴러 id 칼럼 주입"
				result.processStat = s

				step = "메일 헤더에서 프로퍼티 데이터 추출"
				getHeaderInfo(result)

				step = "수신인 세팅(보낸 사람)"
				result.senderNm = initTarget(result.from)[0].senderNm
				result.senderEmail = initTarget(result.from)[0].senderEmail

				step = "수신인 세팅(받은 사람)"
				result.mailTo = getTarget(result.mailTo)

				step = "수신인 세팅(참조)"
				result.mailTo = getTarget(result.cc)

				step = "제목 세팅"
				result.subject = getDecodedSubject(result)

				step = "본문 세팅"
				result.content = getDecodedContents(result)
				result.byteContent = result.content.toByteArray()

				step = "메일함 분류 코드 세팅"
				result.mailboxDtlCd = getMailboxDtlCd(result)

				step = "부재중 메시지 자동 발송"
				sendAbsenceMail(result)

				step = "메일 가공 데이터 저장"
				val y = service.mailDataProcessing(result)

				step = "메일 가공 완료 컬럼 수정"
				if (y > 0) service.mailProUpdate(result)
			} catch (e: Exception) {
				step = "에러데이터 디비에 저장"
				result.errMemo = "mailbox_id(${result.mailboxId}) mail_uid(${result.mailUid}) : ${step}단계에서 에러 발생"
				service.mailDataError(result)

				step = "개발자 메일에 오류 데이터 발신"
				sendErrorMail(result, e)
			}
		}
	}

	fun writeToFile(s: String) {
		service.updateNotProcessedMail(s)
		val processingList: List<WebMailVO> = service.selectNotProcessedMail(s)
		for (result in processingList) {
			try {
				step = "데이터 가공 스케쥴러 id 칼럼 주입"
				result.processStat = s

				step = "메일 헤더에서 프로퍼티 데이터 추출"
				getHeaderInfo(result)

				step = "수신인 세팅(보낸 사람)"
				result.senderNm = initTarget(result.from)[0].senderNm
				result.senderEmail = initTarget(result.from)[0].senderEmail

				step = "수신인 세팅(받은 사람)"
				result.mailTo = getTarget(result.mailTo)

				step = "수신인 세팅(참조)"
				result.mailTo = getTarget(result.cc)

				step = "제목 세팅"
				result.subject = getDecodedSubject(result)

				step = "본문 세팅"
				result.content = getDecodedContents(result)
				result.byteContent = result.content.toByteArray()

				step = "메일함 분류 코드 세팅"
				result.mailboxDtlCd = getMailboxDtlCd(result)

				step = "부재중 메시지 자동 발송"
				sendAbsenceMail(result)

				step = "메일 가공 데이터 저장"
				val y = service.mailDataProcessing(result)

				step = "메일 가공 완료 컬럼 수정"
				if (y > 0) service.mailProUpdate(result)
			} catch (e: Exception) {
				step = "에러데이터 디비에 저장"
				result.errMemo = "mailbox_id(${result.mailboxId}) mail_uid(${result.mailUid}) : ${step}단계에서 에러 발생"
				service.mailDataError(result)

				step = "개발자 메일에 오류 데이터 발신"
				sendErrorMail(result, e)
			}
		}
	}

	private fun initTarget(target: String): List<WebMailVO> {
		val result = ArrayList<WebMailVO>()
		val targets = target.trim { it <= ' ' }.cut("\t", "\r\n").split(",")
		for (group in targets) {
			val from = group.split("<")
			var name = ""
			var email: String
			if (from.size == 1) {
				email = from[0].trim { it <= ' ' }.cut("\t", "\r\n")
			} else {
				name = from[0].trim { it <= ' ' }.cut("\t", "\r\n")
				email = from[1].trim { it <= ' ' }.cut("\t", "\r\n", ">")
			}
			if (group.contains("=?") || group.contains("?=")) {
				name = name.trim { it <= ' ' }.cut(" ", "?=","\"")
				if (name.contains("?B?")) {
					name = name.cut("=?utf-8?B?", "=?UTF-8?B?")
					try {
						name = String(decoder.decode(name.trim { it <= ' ' }), Charset.forName(DEFAULT_CHAR_SET))
					} catch (e: Exception) {
					}
				} else if (name.contains("?Q?")) {
					name = name.cut("=?utf-8?Q?", "=?UTF-8?Q?")
					try {
						name = decoder2.decode(name.trim { it <= ' ' })
					} catch (e: Exception) {
					}
				}
			}
			val vo = WebMailVO()
			vo.senderNm = name
			vo.senderEmail = email
			result.add(vo)
		}
		return result
	}

	private fun getTarget(arg: String): String {
		if (arg != "") {
			val list = initTarget(arg)
			var target = ""
			for (i in list.indices) {
				target += if (!"".equals(list[i].senderNm, ignoreCase = true)) {
					list[i].senderNm + " " + list[i].senderEmail
				} else {
					list[i].senderEmail
				}
				if (i != list.size - 1) target += ", "
			}
			return target
		}
		else {
			return ""
		}
	}

	private fun getHeaderInfo(result: WebMailVO){
		val header: Header?
		var fields: List<Field?>? = null
		try {
			header = Header(ByteArrayInputStream(result.headerBytes))
			fields = header.fields
		} catch (e: IOException) {
			//System.err.println("해더 스트림 오류 else 로");
		}
		if (fields != null) {
			try {
				for (field in fields) {
					field?.let {
						when(it.name) {
							"From" -> result.from = field.body
							"Subject" -> result.subject = field.body
							"Return-Path" -> result.returnPath = field.body
							"To" -> result.mailTo = field.body
							"Cc" -> result.cc = field.body
							"Bcc" -> result.bcc = field.body
						}
					}
				}
			} catch (e: Exception) {		// 제목 75자 넘으면 에러 -> 제목만 따로 처리
				for (field in fields) {
					field?.let {
						when(it.name) {
							"From" -> result.from = field.body
							"Return-Path" -> result.returnPath = field.body
							"To" -> result.mailTo = field.body
							"Cc" -> result.cc = field.body
							"Bcc" -> result.bcc = field.body
						}
					}
				}
				val sub: String = result.subject
				val subs = sub.split("\r\n")
				var subject: String? = ""
				for (s in subs) {
					if (s.startsWith("=?") && s.endsWith("?=")) {
						subject += s
					}
				}
				subject?.let { result.subject = it }
			}
		} else {
			//System.err.println(result.getMailboxId()+"/"+result.getMailUid());
			val sub: String = result.subject
			val subs = sub.split("\r\n")
			var subject = ""
			for (s in subs) {
				if (s.startsWith("=?") && s.endsWith("?=")) {
					subject += """
					$s
					
					""".trimIndent()
				}
			}
			result.subject = subject

//				From: consulting-team mangonetwork <mangonetwork@mangonw.com>
			val mailHeader: String = result.header
			val from = mailHeader.split("From: ")[1].split("\r\n")[0]
			result.from = from

//				Return-Path: <mangonetwork@mangonw.com>
			val rPath = mailHeader.split("Return-Path: ")[1].split("\r\n")[0]
			result.returnPath = rPath

//				To: =?UTF-8?B?7Iug7Jqw7LKgMg==?= <test@letsgotrip.com>
			val tmp1 = mailHeader.split("To:")[1].split("\r\n")
			var mailTo = ""
			for (to in tmp1) {
				mailTo += if (to.contains("\t") || to.startsWith(" ")) {
					to
				} else {
					break
				}
			}
			result.mailTo = mailTo

//				Cc: =?UTF-8?B?6rmA7ZqM7KSA?= <rlaghlwns@letsgotrip.com>, admin@letsgotrip.com
			if (mailHeader.contains("Cc: ")) {
				val tmp2 = mailHeader.split("Cc:")[1].split("\r\n")
				var ccs: String? = ""
				for (cc in tmp2) {
					ccs += if (cc.contains("\t") || cc.startsWith(" ")) {
						cc
					} else {
						break
					}
				}
				ccs?.let{ result.cc = it }
			}
		}
	}

	private fun initAtcFile(mailboxId: String, mailUid: String, fileData: String) {
		step += "(Atc)"
		if (fileData.contains("Content-Disposition: inline;")) {
			step += "(InlineContent)"
		}
		var inlineYn = "N"
		var systemMail = "N"
		var cid: String? = null
		if (fileData.contains("Content-Disposition: inline;")) {
			inlineYn = "Y"
			cid = fileData.split("Content-ID: <")[1].split(">")[0]
		}
		if (fileData.contains("Content-Description") || fileData.isRfc822()) {
			systemMail = "Y"
		}
		var atcFileName: String
		if (fileData.contains("=?") && fileData.contains("?=")) {
			atcFileName = fileData.split("filename=\"")[1].split("\"")[0]
			atcFileName = atcFileName.cut("=?", "?=")
			atcFileName = atcFileName.cut("\t", " ")
			atcFileName = atcFileName.cut("UTF-8?B?", "utf-8?B?")
			val atcFileNameS = atcFileName.split("\r\n")
			atcFileName = ""
			for (namePs in atcFileNameS) {
				var fname = ""
				try {
					fname = String(decoder.decode(namePs), Charset.forName(DEFAULT_CHAR_SET))
				} catch (e: Exception) {
					fname += namePs
					fname += ".txt"
				}
				atcFileName += fname
			}
		} else {
			atcFileName = fileData.split("filename=")[1].split("\r\n")[0]
		}
		atcFileName = atcFileName.cut("\"")
		val encType = fileData.split("Content-Transfer-Encoding: ")[1].split(" ")[0].split("\\;")[0].split("\r\n")[0]

		var filed: String?
		if (encType.equals("base64", ignoreCase = true)) {
			filed = fileData.split("\r\n\r\n")[1].split("--")[0].cut("\r\n")
		} else {
			val sArr = fileData.split("\r\n\r\n")
			filed = ""
			for (i in 1 until sArr.size) {
				filed += "\r\n\r\n${sArr[i]}"
			}
		}
		if (filed != "") {
			val dateFormat = SimpleDateFormat("/yyyy/MM/")
			val currentDate = dateFormat.format(Date())
			val fileCode = UUID.randomUUID().toString().cut("-")
			var filePath = MAIL_ATC_PATH
			filePath += currentDate
			val fileUrl = if(inlineYn == "Y") {
				"${rootUrl}/inline$currentDate"
			} else {
				"${rootUrl}/download$currentDate"
			}
			val filename = atcFileName.split("\\.").toTypedArray()[0]
			var extension = ""
			if (atcFileName.split("\\.").toTypedArray().size != 1) {
				extension = "." + atcFileName.split("\\.").toTypedArray()[1]
			}
			if (!File(filePath).exists()) File(filePath).mkdirs()
			var data: ByteArray? = null
			if (systemMail == "Y") {
				data = filed.toByteArray()
			} else if (encType.equals("7bit", ignoreCase = true)) {
				data = filed.toByteArray()
			} else if (encType.equals("quoted-printable", ignoreCase = true)) {
				var dataString: String? = ""
				try {
					dataString = decoder2.decode(filed, DEFAULT_CHAR_SET)
				} catch (e: Exception) {
					e.printStackTrace()
				}
				if (dataString != null) {
					data = dataString.toByteArray()
				}
			} else {
				data = DatatypeConverter.parseBase64Binary(filed)
			}
			val file = File(filePath + fileCode + extension)
			data?.let {
				val fos = FileOutputStream(file)
				fos.write(data)
				fos.close()
			}

			// 디비에 파일 정보 저장
			val insert = WebMailVO()
			insert.mailboxId = mailboxId
			insert.mailUid = mailUid
			insert.fileName = filename + extension
			insert.fileUrl = fileUrl
			insert.filePath = filePath
			insert.fileCode = fileCode + extension
			insert.fileSize = file.length().toString()
			insert.inlineYn = inlineYn
			cid?.let{ insert.cid = it }
			insert.systemMail = systemMail
			service.insertMailAtcFile(insert)
		}
	}

	private fun getDecodedSubject(result: WebMailVO): String {
		var subject = result.subject
		if (subject.contains("=?") && subject.contains("?=")) {
			val subjects = subject.split("\r\n").toMutableList()
			(0 until subjects.size).forEach {
				if(subjects[it].contains("=?") && subjects[it].contains("?=")) {
					val subs = subjects[it].split("?").toMutableList()
					val charSet = subs[1]
					val encSet = subs[2]
					val subText = subs[3]
					if (encSet.equals("B", ignoreCase = true)) {            // base64
						try {
							subjects[it] = String(decoder.decode(subText), Charset.forName(charSet))
						} catch (e: java.lang.Exception) {
							e.printStackTrace()
						}
					} else if (encSet.equals("Q", ignoreCase = true)) {        // quoted-printable
						try {
							subjects[it] = decoder2.decode(subText)
						} catch (e: java.lang.Exception) {
							e.printStackTrace()
						}
					}
				}
			}
			if (subjects.size > 1) {
				subject = ""
				for (sub in subjects) {
					subject += sub
				}
			} else {
				subject = subjects[0]
			}
		}
		return subject
	}

	private fun getDecodedContents(result: WebMailVO): String {
		var content = ""
		result.isSimpleMail = result.boundary == ""
		if (!result.isSimpleMail) {
			val contentB = result.mail.split(result.boundary)
			for (s in contentB) {
				if ((s.isMultipart() || s.hasBoundary()) && !s.isRfc822()) {
					step += "(innerMail)"
					val innerBoundary = "--${s.split("boundary=\"")[1].split("\"")[0]}"
					val innerMail = s.split(innerBoundary)
					for (m in innerMail) {
						step += "(innerMail)"
						if ((m.contains("text/html") || m.contains("text/plain")) && !m.isContentDisposition()) {
							content = m
							result.enc = m.isEncoded()
							result.encStr = m.getEncodingType()
						} else if (m.isContentDisposition()) {
							initAtcFile(result.mailboxId, result.mailUid, m)
						}
					}
				} else if ((s.contains("text/html") || s.contains("text/plain")) && !s.isContentDisposition()) {
					step += "(Mail)"
					content = s
					result.enc = s.isEncoded()
					result.encStr = s.getEncodingType()
				} else if (s.contains("Content-Type: message/rfc822")) {        // 반송 이메일 정보
					step += "(반송 이메일 Atc)"
					try {
						initAtcFile(result.mailboxId, result.mailUid, s)
					} catch (e: Exception) { /*시스템 메일 디코딩 후 본문에 반송메일 내용 표시할지, 아니면 메모장으로 첨부파일 할지 고쳐야함 지금은 에러뜸*/
					}
				} else if (s.isContentDisposition()) {
					initAtcFile(result.mailboxId, result.mailUid, s)
				}
			}
		}

		step = "본문 세팅(디코딩)"
		if(result.isSimpleMail) {
			when(result.encType) {
				ENC_B -> {
					content = result.mail.trim().cut("\r\n", "\t")
					content = String(decoder.decode(content), Charset.forName(DEFAULT_CHAR_SET))
				}
				ENC_Q -> {
					content = decoder2.decode(result.mail, DEFAULT_CHAR_SET)
				}
			}
		} else if (content != "") {
			if (result.enc && result.encStr.equals(ENC_B, ignoreCase = true)) {
				content = content.split("\r\n\r\n")[1].split("--")[0]
				content = content.trim().cut("\r\n", "\t")
				content = String(decoder.decode(content), Charset.forName(DEFAULT_CHAR_SET))
			} else if (result.enc && result.encStr.equals(ENC_Q, ignoreCase = true)) {
				val contents = content.split("\r\n\r\n")
				content = ""
				var charSet = DEFAULT_CHAR_SET
				for (i in contents.indices) {
					if (i == 0) {
						charSet = contents[i].split("charset=")[1].split("\r\n")[0].cut("\"")
					} else {
						content += contents[i]
					}
				}
				content = content.substring(0, content.length - 2)
				content = decoder2.decode(content, charSet)
			} else if (result.enc && result.encStr.equals(ENC_7, ignoreCase = true)) {
				if (content.contains("Content-Type")) {
					val contents = content.split("\r\n\r\n")
					content = ""
					for (i in 1 until contents.size) {
						content += contents[i]
					}
				}
				content = content.substring(0, content.length - 2)

				val inputStrem: InputStream = MimeUtility.decode(ByteArrayInputStream(content.toByteArray()), ENC_7)
				val buffer = StringBuffer()
				val b = ByteArray(content.toByteArray().size)
				var i: Int
				while (inputStrem.read(b).also { i = it } != -1) {
					buffer.append(String(b, 0, i))
				}
				content = buffer.toString()
			} else {
				content = content.split("\r\n\r\n")[1].split("--")[0]
			}
		}
		else {
			content = "내용 없음"
		}
		return content
	}

	private fun getMailboxDtlCd(result: WebMailVO): String {
		var mailboxDtlCd = "1"
		result.email = result.userName // webMailVO -> MailboxInfoVO 데이터 필드 맞춤
		if (result.mailboxName.equals("Sent", ignoreCase = true)) {
			mailboxDtlCd = "2"
		}
		else {	// 분류 순서 : 받은편지함 -> 스팸 -> 공통 스팸
			// 받은편지함
			var boxList = service.selectBoxSetting(result)
			boxList.filter{ it.mailboxCd != "5" }.forEach { set ->
				selectMailboxDtlCd(set, result)?.let { cd -> mailboxDtlCd = cd }
			}
			// 스팸
			boxList.filter{ it.mailboxCd == "5" }.forEach { set ->
				selectMailboxDtlCd(set, result)?.let { cd -> mailboxDtlCd = cd }
			}
			// 공통 스팸
			val common = WebMailVO()
			common.email = "COMMON"
			boxList = service.selectBoxSetting(common)
			boxList.filter{ it.mailboxCd == "5" }.forEach { set ->
				selectMailboxDtlCd(set, result)?.let { cd -> mailboxDtlCd = cd }
			}
		}
		return mailboxDtlCd
	}

	private fun selectMailboxDtlCd(set: MailboxInfoVO, result: WebMailVO): String? {
		var mailboxDtlCd: String? = null
		when(set.setNm) {
			"제목" -> {
				if (result.subject.contains(set.setVal, ignoreCase = true)) {
					mailboxDtlCd = set.mailboxCd
				}
			}
			"내용" -> {
				if (result.content.contains(set.setVal, ignoreCase = true)) {
					mailboxDtlCd = set.mailboxCd
				}
			}
			"보낸이" -> {
				if (result.senderNm.contains(set.setVal, ignoreCase = true)) {
					mailboxDtlCd = set.mailboxCd
				} else if (result.senderEmail.contains(set.setVal, ignoreCase = true)) {
					mailboxDtlCd = set.mailboxCd
				}
			}
			"참조" -> {
				if (result.cc != "" && result.cc.contains(set.setVal, ignoreCase = true)) {
					mailboxDtlCd = set.mailboxCd
				}
			}
		}
		return mailboxDtlCd
	}

	private fun sendAbsenceMail(result: WebMailVO) {
		service.absenceUser(result)?.let {
			MailUtil.setFieldByWebMailVO(it).send()
		}
	}

	private fun sendErrorMail(errorVO: WebMailVO, e: Exception) {
		val content = "<h1>${errorVO.errMemo}</h1><hr>${e.printStackTrace()}"
			.replace("\n","<br>")
			.replace("\t","&nbsp;&nbsp;&nbsp;&nbsp;")
		MailUtil.subject(errorVO.errMemo)
			.contents(content)
			.senderEmail("$POST_MASTER@$MAIL_DOMAIN")
			.senderNm(POST_MASTER)
			.receiverEmail(DEVELOPER_MAIL)
			.send()
	}

	private fun String.isMultipart(): Boolean {
		return this.contains("Content-Type: multipart")
	}

	private fun String.hasBoundary(): Boolean {
		return this.contains("boundary")
	}

	private fun String.isRfc822(): Boolean {
		return this.contains("message/rfc822")
	}

	private fun String.isEncoded(): Boolean {
		return when {
			this.contains("$CTE: $ENC_7", ignoreCase = true) -> true
			this.contains("$CTE: $ENC_Q", ignoreCase = true) -> true
			this.contains("$CTE: $ENC_B", ignoreCase = true) -> true
			else -> false
		}
	}

	private fun String.getEncodingType(): String {
		return when {
			this.contains("$CTE: $ENC_7", ignoreCase = true) -> ENC_7
			this.contains("$CTE: $ENC_Q", ignoreCase = true) -> ENC_Q
			this.contains("$CTE: $ENC_B", ignoreCase = true) -> ENC_B
			else -> ""
		}
	}

	private fun String.isContentDisposition(): Boolean {
		return this.contains("Content-Disposition")
	}

	private fun String.cut(vararg str: String): String {
		var result = this
		str.forEach {
			result = result.replace(it,"")
		}
		return result
	}

	private companion object {
		private val decoder = Base64.getDecoder()
		private val decoder2 = QuotedPrintableCodec()
		private const val DEFAULT_CHAR_SET = "utf-8"
		private const val MAIL_ATC_PATH = "/usr/local/mailAtcFile"
		private const val CTE = "Content-Transfer-Encoding"
		private const val ENC_7 = "7bit"
		private const val ENC_Q = "quoted-printable"
		private const val ENC_B = "base64"
		private const val POST_MASTER = "letsgotrip"
		private const val MAIL_DOMAIN = "letsgotrip.com"
		private const val DEVELOPER_MAIL = "mangonetwork@mangonw.com"
	}

}