package com.mangonw.server.mail

data class WebMailVO(
	var from: String = "",
	var senderNm: String = "",
	var senderEmail: String = "",

	var headerBytes: ByteArray = byteArrayOf(),

	var subject: String = "",
	var returnPath: String = "",
	var mailTo: String = "",
	var cc: String = "",
	var bcc: String = "",

	var header: String = "",

	var mailboxId: String = "",
	var mailUid: String = "",
	var fileName: String = "",
	var filePath: String = "",
	var fileCode: String = "",
	var fileSize: String = "",
	var fileUrl: String = "",
	var inlineYn: String = "",
	var cid: String = "",
	var systemMail: String = "",

	var boundary: String = "",
	var mail: String = "",
	var encType: String = "",
	var content: String = "",
	var byteContent: ByteArray = byteArrayOf(),

	var email: String = "",
	var userName: String = "",
	var mailboxName: String = "",

	var mailboxDtlCd: String = "",

	var errMemo: String = "",

	var receiverEmail: String = "",
	var absenceMsgSub: String = "",
	var absenceMsgCtx: String = "",

	var referenceEmail: String = "",
	var hiddenReferenceEmail: String = "",

	var mailIsAnswered: String = "",
	var mailIsDeleted: String = "",
	var mailDate: String = "",
	var mailIsSeen: String = "",
	var mailBytes: ByteArray = byteArrayOf(),
	var hasAtc: String = "",
	var subjects: String = "",

	var retrievalTarget: String = "",

	var yyyy: String = "",
	var mm: String = "",
	var absenceYn: String = "",
	var absenceFromDt: String = "",
	var absenceToDt: String = "",

	var isSimpleMail: Boolean = false,
	var enc: Boolean = false,
	var encStr: String = "",
	var processStat: String = "",
	var rn: Int = 0,
	var singlePart: Boolean = true,
	var uuid: String = "",
	var encodeYn: String = "N",
)
