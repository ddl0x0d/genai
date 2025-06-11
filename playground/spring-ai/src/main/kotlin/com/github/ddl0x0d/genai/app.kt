package com.github.ddl0x0d.genai

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringAIApplication

fun main() {
    runApplication<SpringAIApplication> {
        webApplicationType = WebApplicationType.NONE
    }
}
