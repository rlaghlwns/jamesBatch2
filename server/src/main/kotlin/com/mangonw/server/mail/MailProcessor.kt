package com.mangonw.server.mail

import com.mangonw.server.home.Service
import com.mangonw.server.mail.sender.MailUtil
import org.apache.commons.codec.net.QuotedPrintableCodec
import org.apache.james.mime4j.codec.Base64InputStream
import org.apache.james.mime4j.codec.QuotedPrintableInputStream
import org.apache.james.mime4j.parser.*
import org.apache.james.mime4j.stream.BodyDescriptor
import org.apache.james.mime4j.stream.Field
import org.apache.james.mime4j.util.MimeUtil
import org.simplejavamail.api.email.Email
import org.simplejavamail.converter.EmailConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors
import javax.mail.internet.MimeMessage
import javax.xml.bind.DatatypeConverter


@Component
class MailProcessor {

	@Value("http://\${server.ip}:\${server.port}")
	private lateinit var rootUrl: String

	@Autowired
	private lateinit var service: Service

	private var step = ""

	suspend fun writeToFile2(s: String, processingList: List<WebMailVO>) {
		for (result in processingList) {
			try {

				//val iss = ByteArrayInputStream(result.headerBytes)
				//val email = EmailConverter.emlToMimeMessage(result.mail)
				//email.fromRecipient
				//email.subject
				//email.plainText
				//email.attachments
				//email.embeddedImages
				//email.headers


				step = "데이터 가공 스케쥴러 id 칼럼 주입"
				result.processStat = s

				step = "메일 헤더에서 프로퍼티 데이터 추출"
				getHeaderInfo(result)

				step = "본문 세팅"
				if(result.systemMail != "Y") {
					result.content = getDecodedContents(result)
					result.byteContent = result.content.toByteArray()
				} else {
					result.content = result.mail
					result.byteContent = result.mail.toByteArray()
				}

				//System.err.println(result.from)

				step = "메일함 분류 코드 세팅"
				result.mailboxDtlCd = getMailboxDtlCd(result)

				step = "부재중 메시지 자동 발송"
				sendAbsenceMail(result)

				step = "메일 가공 데이터 저장"
				val y = service.mailDataProcessing(result)

				step = "메일 가공 완료 컬럼 수정"
				if (y > 0) service.mailProUpdate(result)
			} catch (e: Exception) {
				e.printStackTrace()
				//step = "에러데이터 디비에 저장"
				result.errMemo = "mailbox_id(${result.mailboxId}) mail_uid(${result.mailUid}) : ${step}단계에서 에러 발생"
				service.mailDataError(result)

				step = "개발자 메일에 오류 데이터 발신"
				sendErrorMail(result, e)
			}
		}
	}

	private fun getHeaderInfo(result: WebMailVO){
		val handler = MailHeaderHandler(1, result)
		val parser = MimeStreamParser()
		parser.setContentHandler(handler)
		ByteArrayInputStream(result.headerBytes).use {
			parser.parse(it)
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
					e.printStackTrace()
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

	private fun getDecodedContents2(result: WebMailVO): String {
		val handler = MailHandler(1, result)
		val parser = MimeStreamParser()
		parser.setContentHandler(handler)
		ByteArrayInputStream(result.mailBytes).use {
			parser.parse(it)
		}
		return handler.content
	}
	private fun getDecodedContents(result: WebMailVO): String {
		result.isSimpleMail = result.boundary == ""
		if (!result.isSimpleMail) {
			val contentB = result.mail.split(result.boundary)
			for (s in contentB) {
				if ((s.isMultipart() || s.hasBoundary()) && !s.isRfc822()) {
					result.singlePart = false
					step += "(innerMail)"
					val innerBoundary = "--${s.split("boundary=\"")[1].split("\"")[0]}"
					val innerMail = s.split(innerBoundary)
					for (m in innerMail) {
						step += "(innerMail)"
						if ((m.contains("text/html") || m.contains("text/plain")) && !m.isContentDisposition()) {
							result.enc = m.isEncoded()
							result.encStr = m.getEncodingType()
						} else if (m.isContentDisposition()) {
							initAtcFile(result.mailboxId, result.mailUid, m)
						}
					}
				}
				else if ((s.contains("text/html") || s.contains("text/plain")) && !s.isContentDisposition()) {
					step += "(Mail)"
					result.enc = s.isEncoded()
					result.encStr = s.getEncodingType()
				}
				else if (s.contains("Content-Type: message/rfc822")) {        // 반송 이메일 정보
					step += "(반송 이메일 Atc)"
					try {
						initAtcFile(result.mailboxId, result.mailUid, s)
					} catch (e: Exception) { /*시스템 메일 디코딩 후 본문에 반송메일 내용 표시할지, 아니면 메모장으로 첨부파일 할지 고쳐야함 지금은 에러뜸*/
						e.printStackTrace()
					}
				}
				else if (s.isContentDisposition()) {
					initAtcFile(result.mailboxId, result.mailUid, s)
				}
			}
		}

		/*step = "본문 세팅(디코딩)"
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
		return content*/
		var ccc = if(result.isSimpleMail) {
			when(result.encType) {
				ENC_B -> {
					result.mail = result.mail.trim().cut("\r\n", "\t")
					String(decoder.decode(result.mail), Charset.forName(DEFAULT_CHAR_SET))
				}
				ENC_Q -> {
					decoder2.decode(result.mail, DEFAULT_CHAR_SET)
				}
				else -> {
					result.mail
				}
			}
		}
		else if (result.mail == "") {
			""
		}
		else {
			getDecodedContents2(result)
		}

		/* 남은 첨부 없나... */
		//System.err.println(result.from)
		if (result.from.contains("samsung.com")) {
			if (result.mail.contains("Content-ID")) {
				val contentB = result.mail.split(result.boundary)
				contentB.forEach {
					if(it.contains("Content-ID")) {
						/*val iit = it.split("\r\n\r\n")
						val cid = iit[0].split("Content-ID: <")[1].split(">")[0]
						val extension = iit[0].split("Content-Type: ")[1].split("\r\n")[0].split("/")[1]
						val src = "data:image/$extension;base64,${iit[1].replace("\r\n","").replace("\t","").replace(" ","")}"
						val orgSrc = "cid:$cid"
						ccc = ccc.replace(orgSrc,src)*/
					}
				}
			}
		}
		if (result.from.contains("jcint.co.kr")) {
			val handler = MailHandler2(1, result)
			val parser = MimeStreamParser()
			parser.setContentHandler(handler)
			ByteArrayInputStream(result.mailBytes).use {
				parser.parse(it)
			}
		}

		return ccc
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

class MailHeaderHandler(private val i: Int, private val result: WebMailVO): ContentHandler {

	private fun targetInit(bd: String): WebMailVO {
		val vo = WebMailVO()
		val from = bd.split("<")
		var name = ""
		var email = ""
		if (from.size == 1) {
			email = from[0].trim().cut("\t", "\r\n")
		} else {
			name = from[0].cut("\t", "\r\n", " ")
			email = from[1].cut("\t", "\r\n", " ", ">")
		}

		val encoded = name.contains("=?") && name.contains("?=")
		if (encoded) {
			val charSet = name.split("?")[1]
			val encSet = name.split("?")[2]
			val f = "=?$charSet?$encSet?"
			val bodyArr = name.cut("\t","\r\n"," ", f).split("?=")
			if (encSet.equals("B", ignoreCase = true)) {
				try {
					name = ""
					bodyArr.forEach {
						name += Base64InputStream(ByteArrayInputStream(it.toByteArray()))
							.bufferedReader(Charset.forName(charSet)).lines().collect(Collectors.joining())
					}
				} catch (e: java.lang.Exception) {}
			} else if (encSet.equals("Q", ignoreCase = true)) {
				try {
					name = ""
					bodyArr.forEach {
						name += QuotedPrintableInputStream(ByteArrayInputStream(it.toByteArray()))
							.bufferedReader(Charset.forName(charSet)).lines().collect(Collectors.joining())
					}
				} catch (e: java.lang.Exception) {}
			}
		}
		vo.senderNm = name
		vo.senderEmail = email
		return vo
	}

	override fun field(field: Field?) {
		field?.let {
			when(it.name) {
				"From" -> {
					val target = targetInit(it.body)
					result.senderNm = target.senderNm
					result.senderEmail = target.senderEmail
				}
				"Subject" -> {
					result.subject = it.body
					val encoded = it.body.contains("=?") && it.body.contains("?=")
					if(encoded) {
						val subConf = it.body.contains(": ")
						var subConfNm = ""
						if(subConf) {
							subConfNm = "${ it.body.split(": ")[0] }: "
						}
						val charSet = it.body.split("?")[1]
						val encSet = it.body.split("?")[2]
						val f = "=?$charSet?$encSet?"
						if (encSet.equals("B", ignoreCase = true)) {
							try {
								val bodyArr = it.body.cut(subConfNm, f,"\t","\r\n"," ").split("?=")
								var subj = ""
								bodyArr.forEach {
									subj += Base64InputStream(ByteArrayInputStream(it.toByteArray()))
										.bufferedReader(Charset.forName(charSet)).lines().collect(Collectors.joining())
								}
								result.subject = subConfNm+subj
								if(result.subject.contains("�")) {
									result.subject = subConfNm+Base64InputStream(ByteArrayInputStream(it.body.cut(subConfNm, f,"\t","\r\n"," ","?=").toByteArray()))
                                        .bufferedReader(Charset.forName(charSet)).lines().collect(Collectors.joining())
								}
							} catch (e: java.lang.Exception) {}
						} else if (encSet.equals("Q", ignoreCase = true)) {
							try {
								val bodyArr = it.body.cut(subConfNm, f,"\t","\r\n"," ").split("?=")
								var subj = ""
								bodyArr.forEach {
									subj += QuotedPrintableInputStream(ByteArrayInputStream(it.toByteArray()))
										.bufferedReader(Charset.forName(charSet)).lines().collect(Collectors.joining())
								}
								result.subject = subConfNm+subj
								if(result.subject.contains("�")) {
									result.subject = subConfNm+QuotedPrintableInputStream(ByteArrayInputStream(it.body.cut(subConfNm, f,"\t","\r\n"," ","?=").toByteArray()))
										.bufferedReader(Charset.forName(charSet)).lines().collect(Collectors.joining())
								}
							} catch (e: java.lang.Exception) {}
						}
					}
				}
				"Return-Path" -> {
					if(it.body.isNullOrBlank() or it.body.equals("<>")) {
						result.systemMail = "Y"
					}
					result.returnPath = it.body
				}
				"To" -> {
					val list = ArrayList<WebMailVO>()
					val targets = it.body.trim().cut("\t", "\r\n").split(",")
					for (group in targets) {
						val target = targetInit(it.body)
						val vo = WebMailVO()
						vo.senderNm = target.senderNm
						vo.senderEmail = target.senderEmail
						list.add(vo)
					}

					var target = ""
					for (i in list.indices) {
						target += if (!"".equals(list[i].senderNm, ignoreCase = true)) {
							list[i].senderNm + " " + list[i].senderEmail
						} else {
							list[i].senderEmail
						}
						if (i != list.size - 1) target += ", "
					}
					result.mailTo = target
				}
				"Cc" -> {
					val list = ArrayList<WebMailVO>()
					val targets = it.body.trim().cut("\t", "\r\n").split(",")
					for (group in targets) {
						val target = targetInit(it.body)
						val vo = WebMailVO()
						vo.senderNm = target.senderNm
						vo.senderEmail = target.senderEmail
						list.add(vo)
					}

					var target = ""
					for (i in list.indices) {
						target += if (!"".equals(list[i].senderNm, ignoreCase = true)) {
							list[i].senderNm + " " + list[i].senderEmail
						} else {
							list[i].senderEmail
						}
						if (i != list.size - 1) target += ", "
					}
					result.cc = target
				}
				"Bcc" -> {
					val list = ArrayList<WebMailVO>()
					val targets = it.body.trim().cut("\t", "\r\n").split(",")
					for (group in targets) {
						val target = targetInit(it.body)
						val vo = WebMailVO()
						vo.senderNm = target.senderNm
						vo.senderEmail = target.senderEmail
						list.add(vo)
					}

					var target = ""
					for (i in list.indices) {
						target += if (!"".equals(list[i].senderNm, ignoreCase = true)) {
							list[i].senderNm + " " + list[i].senderEmail
						} else {
							list[i].senderEmail
						}
						if (i != list.size - 1) target += ", "
					}
					result.bcc = target
				}
				"Sendmail-UUID" -> {
					result.uuid = it.body
				}
			}
		}
	}

	override fun startMessage() {
		//System.err.println("${i}. ContentHandler.startMessage")
	}
	override fun endMessage() {
		//System.err.println("${i}. ContentHandler.endMessage")
	}
	override fun startBodyPart() {
		//System.err.println("${i}. ContentHandler.startBodyPart")
	}
	override fun endBodyPart() {
		//System.err.println("${i}. ContentHandler.endBodyPart")
	}
	override fun startHeader() {
		//System.err.println("${i}. ContentHandler.startHeader")
	}
	override fun endHeader() {
		//System.err.println("${i}. ContentHandler.endHeader")
	}
	override fun preamble(ist: InputStream?) { // 전문
		//System.err.println("${i}. ContentHandler.preamble : ")
		ist?.let {
			val ii = i+1
			val handler: ContentHandler = MailHeaderHandler(ii, result)
			val parser = MimeStreamParser()
			parser.setContentHandler(handler)
			ist.use {
				parser.parse(it)
			}
		}
	}
	override fun epilogue(ist: InputStream?) { // 발문
		//System.err.println("${i}. ContentHandler.epilogue : ")
		ist?.let {
			val ii = i+1
			val handler: ContentHandler = MailHeaderHandler(ii, result)
			val parser = MimeStreamParser()
			parser.setContentHandler(handler)
			ist.use {
				parser.parse(it)
			}
		}
	}
	override fun startMultipart(bd: BodyDescriptor?) {
		//System.err.println("${i}. ContentHandler.startMultipart")
	}
	override fun endMultipart() {
		//System.err.println("${i}. ContentHandler.endMultipart")
	}
	override fun body(bd: BodyDescriptor?, ist: InputStream?) {
		ist?.let {
			val isr = InputStreamReader(ist)
			val stream = BufferedReader(isr).lines()
			val s = stream.collect(Collectors.joining())
			//System.err.println("${i}. ContentHandler.body : $s")
		}
	}
	override fun raw(ist: InputStream?) {
		ist?.let {
			val isr = InputStreamReader(ist)
			val stream = BufferedReader(isr).lines()
			val s = stream.collect(Collectors.joining())
			//System.err.println("${i}. ContentHandler.raw : $s")
		}
	}

	private fun String.cut(vararg str: String): String {
		var result = this
		str.forEach {
			result = result.replace(it,"")
		}
		return result
	}

}

class MailHandler(private val i: Int, private val result: WebMailVO): ContentHandler {

	var content = ""
	override fun body(bd: BodyDescriptor?, ist: InputStream?) {

		//System.err.println(bd?.mimeType)

		bd?.mediaType?.let {
			if (it == "text") {
				bd.transferEncoding?.let { t ->
					content = when (t.trim()) {
						MimeUtil.ENC_QUOTED_PRINTABLE -> {
							if (result.singlePart) {
								val list = result.mail.split("--${result.boundary}")
								var res = ""
								list.forEach { l ->
									if (l.contains("Content-Type: text", ignoreCase = true) && l.contains("Content-Transfer-Encoding: quoted-printable", ignoreCase = true)) {
										res = l.split("\r\n\r\n")[1].trim().cut("\r\n", "\t")
										res = QuotedPrintableInputStream(ByteArrayInputStream(res.toByteArray())).reader(Charset.forName(bd.charset)).readText()
									}
								}
								//System.err.println("ENC_QUOTED_PRINTABLE : single : $res")
								res
							}
							else {
								//QuotedPrintableInputStream(ist).bufferedReader(Charset.forName(bd.charset)).lines().collect(Collectors.joining())
								val res = QuotedPrintableInputStream(ist).reader(Charset.forName(bd.charset)).readText()
								//System.err.println("ENC_QUOTED_PRINTABLE : !single : $res")
								res
							}
						}
						MimeUtil.ENC_BASE64 -> {
							if (result.singlePart) {
								val list = result.mail.split("--${result.boundary}")
								var res = ""
								list.forEach { l ->
									if (l.contains("Content-Type: text", ignoreCase = true) && l.contains("Content-Transfer-Encoding: base64", ignoreCase = true)) {
										res = l.split("\r\n\r\n")[1].trim().cut("\r\n", "\t")
										res = Base64InputStream(ByteArrayInputStream(res.toByteArray())).reader(Charset.forName(bd.charset)).readText()
									}
								}
								res = res.replace("\r\n","")
								//System.err.println("ENC_BASE64 : single : $res")
								res.replace("\r\n","")
							}
							else {
								//Base64InputStream(ist).bufferedReader(Charset.forName(bd.charset)).lines().collect(Collectors.joining())
								val res = Base64InputStream(ist).reader(Charset.forName(bd.charset)).readText()
								//System.err.println("ENC_BASE64 : !single : $res")
								res
							}
						}
						else -> {
							if (result.singlePart) {
								val list = result.mail.split("--${result.boundary}")
								var res = ""
								list.forEach { l ->
									if (l.contains("Content-Type: text", ignoreCase = true)) {
										res = l.split("\r\n\r\n")[1].trim().cut("\r\n", "\t")
										if(result.systemMail != "Y") {
											res = BufferedReader(InputStreamReader(ByteArrayInputStream(res.toByteArray()))).lines().collect(Collectors.joining())
										}
									}
								}
								//System.err.println("else : single : $res")
								res
							}
							else {
								val res = BufferedReader(InputStreamReader(ist)).lines().collect(Collectors.joining())
								//System.err.println("else : !single : $res")
								res
							}
						}
					}
				}
			}
		}

	}

	override fun startMessage() {
		//System.err.println("${i}. ContentHandler.startMessage")
	}
	override fun endMessage() {
		//System.err.println("${i}. ContentHandler.endMessage")
	}
	override fun startBodyPart() {
		//System.err.println("${i}. ContentHandler.startBodyPart")
	}
	override fun endBodyPart() {
		//System.err.println("${i}. ContentHandler.endBodyPart")
	}
	override fun startHeader() {
		//System.err.println("${i}. ContentHandler.startHeader")
	}
	override fun field(field: Field?) {
		//System.err.println("${i}. ContentHandler.field : " + field?.name + ":" + field?.body)
	}
	override fun endHeader() {
		//System.err.println("${i}. ContentHandler.endHeader")
	}
	override fun preamble(ist: InputStream?) { // 전문
		//System.err.println("${i}. ContentHandler.preamble : ")
		ist?.let {
			val ii = i+1
			val handler: ContentHandler = MailHandler(ii, result)
			val parser = MimeStreamParser()
			parser.setContentHandler(handler)
			ist.use {
				parser.parse(it)
			}
		}
	}
	override fun epilogue(ist: InputStream?) { // 발문
		//System.err.println("${i}. ContentHandler.epilogue : ")
		ist?.let {
			val ii = i+1
			val handler: ContentHandler = MailHandler(ii, result)
			val parser = MimeStreamParser()
			parser.setContentHandler(handler)
			ist.use {
				parser.parse(it)
			}
		}
	}
	override fun startMultipart(bd: BodyDescriptor?) {
		//System.err.println("${i}. ContentHandler.startMultipart")
	}
	override fun endMultipart() {
		//System.err.println("${i}. ContentHandler.endMultipart")
	}
	override fun raw(ist: InputStream?) {
		ist?.let {
			val isr = InputStreamReader(ist)
			val stream = BufferedReader(isr).lines()
			val s = stream.collect(Collectors.joining())
			System.err.println("${i}. ContentHandler.raw : $s")
		}
	}

	private fun String.cut(vararg str: String): String {
		var result = this
		str.forEach {
			result = result.replace(it,"")
		}
		return result
	}

}

class MailHandler2(private val i: Int, private val result: WebMailVO): ContentHandler {

	override fun body(bd: BodyDescriptor?, ist: InputStream?) {
		//System.err.println(result.boundary)
		result.mail.split("--${result.boundary}").forEach {
			if (it.contains("Content-Type: ")) {
				it.split("Content-Type: ")[1].split(";")[0]
			}
		}
	}

	override fun startMessage() {
		//System.err.println("${i}. ContentHandler.startMessage")
	}
	override fun endMessage() {
		//System.err.println("${i}. ContentHandler.endMessage")
	}
	override fun startBodyPart() {
		//System.err.println("${i}. ContentHandler.startBodyPart")
	}
	override fun endBodyPart() {
		//System.err.println("${i}. ContentHandler.endBodyPart")
	}
	override fun startHeader() {
		//System.err.println("${i}. ContentHandler.startHeader")
	}
	override fun field(field: Field?) {
		//System.err.println("${i}. ContentHandler.field : " + field?.name + ":" + field?.body)
	}
	override fun endHeader() {
		//System.err.println("${i}. ContentHandler.endHeader")
	}
	override fun preamble(ist: InputStream?) { // 전문
		//System.err.println("${i}. ContentHandler.preamble : ")
		ist?.let {
			val ii = i+1
			val handler: ContentHandler = MailHandler(ii, result)
			val parser = MimeStreamParser()
			parser.setContentHandler(handler)
			ist.use {
				parser.parse(it)
			}
		}
	}
	override fun epilogue(ist: InputStream?) { // 발문
		//System.err.println("${i}. ContentHandler.epilogue : ")
		ist?.let {
			val ii = i+1
			val handler: ContentHandler = MailHandler(ii, result)
			val parser = MimeStreamParser()
			parser.setContentHandler(handler)
			ist.use {
				parser.parse(it)
			}
		}
	}
	override fun startMultipart(bd: BodyDescriptor?) {
		//System.err.println("${i}. ContentHandler.startMultipart")
	}
	override fun endMultipart() {
		//System.err.println("${i}. ContentHandler.endMultipart")
	}
	override fun raw(ist: InputStream?) {
		ist?.let {
			val isr = InputStreamReader(ist)
			val stream = BufferedReader(isr).lines()
			val s = stream.collect(Collectors.joining())
			//System.err.println("${i}. ContentHandler.raw : $s")
		}
	}

	private fun String.cut(vararg str: String): String {
		var result = this
		str.forEach {
			result = result.replace(it,"")
		}
		return result
	}

}