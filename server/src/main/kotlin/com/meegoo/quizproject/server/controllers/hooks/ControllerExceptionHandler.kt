package com.meegoo.quizproject.server.controllers.hooks

import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletResponse

@ControllerAdvice
internal class ControllerExceptionHandler {

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException, response: HttpServletResponse) {
        response.sendError(HttpStatus.FORBIDDEN.value(), ex.message)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException, response: HttpServletResponse) {
        response.sendError(HttpStatus.UNAUTHORIZED.value(), ex.message)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, response: HttpServletResponse) {
        response.sendError(HttpStatus.BAD_REQUEST.value(), ex.message)
    }

    @ExceptionHandler(UsernameTakenException::class)
    fun usernameTaken(ex: UsernameTakenException, response: HttpServletResponse) {
        response.sendError(HttpStatus.CONFLICT.value(), "Username is taken")
    }

    @ExceptionHandler(GroupTakenException::class)
    fun groupTaken(ex: GroupTakenException, response: HttpServletResponse) {
        response.sendError(HttpStatus.CONFLICT.value(), "Group name is taken")
    }

    @ExceptionHandler(BadRequestException::class)
    fun badRequest(ex: BadRequestException, response: HttpServletResponse) {
        response.sendError(HttpStatus.BAD_REQUEST.value(), ex.message)
    }

    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException, response: HttpServletResponse) {
        response.sendError(HttpStatus.NOT_FOUND.value(), ex.message)
    }

    @ExceptionHandler(SidNotFoundException::class)
    fun sidNotFound(ex: SidNotFoundException, response: HttpServletResponse) {
        response.sendError(HttpStatus.NOT_FOUND.value(), ex.message)
    }

//    override fun handleExceptionInternal(
//        ex: java.lang.Exception,
//        body: Any?,
//        headers: HttpHeaders,
//        status: HttpStatus,
//        request: WebRequest
//    ): ResponseEntity<Any> {
//        // for all exceptions that are not overriden, the body is null, so we can
//        // just provide new body based on error message and call super method
//        val apiError = (if (Objects.isNull(body)) ApiError(status, exception.message) // <--
//        else body)!!
//        return super.handleExceptionInternal(exception, apiError, headers!!, status!!, request!!)
//    }
}