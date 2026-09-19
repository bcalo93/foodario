package com.foodario.di

import com.foodario.database.DatabaseDriverFactory
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

class KoinGraphTest {

    @Test
    fun `verify koin graph resolves all definitions`() {
        val module = module {
            includes(dataModule, domainModule, presentationModule)
        }
        module.verify(
            extraTypes = listOf(DatabaseDriverFactory::class, Long::class),
        ).verify()
    }
}
