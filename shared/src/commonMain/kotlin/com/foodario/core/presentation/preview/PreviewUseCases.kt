package com.foodario.core.presentation.preview

import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal fun <P> previewUnitUseCase(): UseCase<P, Unit> =
    object : UseCase<P, Unit> {
        override suspend fun invoke(params: P) = Unit
    }

internal fun <P, T> previewObserveUseCase(value: T): ObserveUseCase<P, T> =
    object : ObserveUseCase<P, T> {
        override fun invoke(params: P): Flow<T> = flowOf(value)
    }
