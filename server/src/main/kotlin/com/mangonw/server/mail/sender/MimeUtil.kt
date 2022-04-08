package com.mangonw.server.mail.sender

object MimeUtil {

    private const val PLAIN = "text/plain"
    private const val HTML = "text/html"
    private const val CSS = "text/css"
    private const val JAVASCRIPT = "text/javascript"
    private val text = arrayOf(PLAIN, HTML, CSS, JAVASCRIPT)

    private const val GIF = "image/gif"
    private const val PNG = "image/png"
    private const val JPEG = "image/jpeg"
    private const val BMP = "image/bmp"
    private const val WEBP = "image/webp"
    private val image = arrayListOf(GIF, PNG, JPEG, BMP, WEBP)

    private const val MIDI = "audio/midi"
    private const val MPEG = "audio/mpeg"
    private const val WEBM = "audio/webm"
    private const val OGG = "audio/ogg"
    private const val WAV = "audio/wav"
    private val audio = arrayListOf(MIDI, MPEG, WEBM, OGG, WAV)

    private val video = arrayListOf("")

    private val application = arrayListOf("")

}