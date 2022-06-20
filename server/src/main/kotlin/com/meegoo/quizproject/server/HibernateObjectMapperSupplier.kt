package com.meegoo.quizproject.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.vladmihalcea.hibernate.type.util.ObjectMapperSupplier
import java.util.*


class HibernateObjectMapperSupplier : ObjectMapperSupplier {


    override fun get(): ObjectMapper {

        val objectMapper = ObjectMapper().findAndRegisterModules()
        val simpleModule = JacksonConfig().customJacksonModule()
        objectMapper.registerModule(simpleModule)
        return objectMapper

    }
}