package com.foodario.core.presentation.preview

import com.foodario.core.domain.usecase.UseCase

internal fun <P> previewUnitUseCase(): UseCase<P, Unit> =
    object : UseCase<P, Unit> {
        override suspend fun invoke(params: P) = Unit
    }
