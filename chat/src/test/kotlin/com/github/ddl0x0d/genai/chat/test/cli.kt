package com.github.ddl0x0d.genai.chat.test

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.testing.CliktCommandTestResult
import com.github.ajalt.clikt.testing.test
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream

fun CliktCommand.test(argv: String = "", stdin: () -> String): CliktCommandTestResult {
    val original = System.`in`
    val buf: ByteArray = stdin().trimIndent().toByteArray()
    val test = ByteArrayInputStream(buf)
    System.setIn(test)
    return try {
        test(argv)
    } finally {
        System.setIn(original)
    }
}

fun CliktCommandTestResult.shouldSucceed(expectedStdout: () -> String) {
    shouldComplete(expectedStatusCode = 0, expectedStdout, expectedStderr = { "" })
}

fun CliktCommandTestResult.shouldFail(expectedStatusCode: Int = 1, expectedStderr: () -> String) {
    shouldComplete(expectedStatusCode, expectedStdout = { "" }, expectedStderr)
}

fun CliktCommandTestResult.shouldComplete(
    expectedStatusCode: Int,
    expectedStdout: () -> String,
    expectedStderr: () -> String,
) {
    assertSoftly {
        statusCode shouldBe expectedStatusCode
        stdout shouldBe expectedStdout()
        stderr shouldBe expectedStderr()
    }
}
