package com.mangonw.server.interceptor

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class RequestInterceptor : HandlerInterceptor {
    @Value("\${server.apiKey}")
    private lateinit var apiKey: String

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val headerKey = request.getHeader("apiKey")
        /*if(apiKey != headerKey) {
            response.status = 403
            return false
        }*/
        return true
    }
}