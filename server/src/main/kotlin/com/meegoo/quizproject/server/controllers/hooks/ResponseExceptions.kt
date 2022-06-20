package com.meegoo.quizproject.server.controllers.hooks

import com.meegoo.quizproject.server.security.acl.GroupSid
import org.springframework.security.acls.model.Sid
import java.lang.RuntimeException
import java.util.*

class BadRequestException(message: String) : RuntimeException(message)

class UnauthorizedException(message: String) : RuntimeException(message)

class NotFoundException(className: String, uuid: UUID) : RuntimeException("$className with UUID $uuid not found")

class SidNotFoundException() : RuntimeException("Sid not found")

class UsernameTakenException : RuntimeException()

class GroupTakenException : RuntimeException()