package com.foodario.scanning.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.foodario.core.domain.matching.MatchConfidence
import com.foodario.core.domain.matching.ProductMatch
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.scanning.domain.model.ScannedReceiptItem
import com.foodario.scanning.domain.service.ReceiptTextRecognizer
import com.foodario.scanning.domain.usecase.AddFoodItemsParams
import com.foodario.scanning.domain.usecase.MatchProductsParams
import com.foodario.scanning.domain.usecase.ParseReceiptParams
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptScanViewModelTest {

    private val receiptTextRecognizer = mockk<ReceiptTextRecognizer>()
    private val parseReceipt = mockk<UseCase<ParseReceiptParams, List<ScannedReceiptItem>>>()
    private val matchProducts = mockk<UseCase<MatchProductsParams, List<ProductMatch?>>>()
    private val addFoodItems = mockk<UseCase<AddFoodItemsParams, List<FoodItem>>>()

    private val lecheItem = FoodItem(
        id = 1L,
        name = "Leche",
        category = FoodCategory.DAIRY,
        quantity = 2.0,
        unit = QuantityUnit.UNIT,
        isFrozen = false,
        expirationDate = null,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    private fun scannedItem(
        name: String = "Leche",
        category: FoodCategory = FoodCategory.DAIRY,
        quantity: Double = 1.0,
        unit: QuantityUnit = QuantityUnit.UNIT,
    ) = ScannedReceiptItem(
        name = name,
        category = category,
        quantity = quantity,
        unit = unit,
        rawLine = name,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `photo captured happy path moves to reviewing with items and matches`() = runTest {
        coEvery { receiptTextRecognizer.recognizeText(any()) } returns "LECHE 1LT"
        coEvery { parseReceipt(any()) } returns listOf(scannedItem())
        coEvery { matchProducts(any()) } returns listOf(ProductMatch(lecheItem, MatchConfidence.EXACT))

        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onEvent(ReceiptScanEvent.PhotoCaptured(byteArrayOf(1, 2, 3)))
            val state = awaitStep(ReceiptScanStep.Reviewing)

            assertEquals(1, state.items.size)
            assertEquals("Leche", state.items[0].name)
            assertNotNull(state.items[0].match)
            assertEquals(lecheItem.id, state.items[0].mergeIntoItemId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ocr failure returns to capturing with error`() = runTest {
        coEvery { receiptTextRecognizer.recognizeText(any()) } throws RuntimeException("boom")

        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onEvent(ReceiptScanEvent.PhotoCaptured(byteArrayOf(1, 2, 3)))
            val state = awaitErrorState()

            assertEquals(ReceiptScanStep.Capturing, state.step)
            assertNotNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty parse result shows error and stays in capturing`() = runTest {
        coEvery { receiptTextRecognizer.recognizeText(any()) } returns "TOTAL"
        coEvery { parseReceipt(any()) } returns emptyList()

        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onEvent(ReceiptScanEvent.PhotoCaptured(byteArrayOf(1, 2, 3)))
            val state = awaitErrorState()

            assertEquals(ReceiptScanStep.Capturing, state.step)
            assertNotNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle included updates count`() = runTest {
        coEvery { receiptTextRecognizer.recognizeText(any()) } returns "LECHE 1LT"
        coEvery { parseReceipt(any()) } returns listOf(scannedItem())
        coEvery { matchProducts(any()) } returns listOf(null)

        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onEvent(ReceiptScanEvent.PhotoCaptured(byteArrayOf(1, 2, 3)))
            val reviewing = awaitStep(ReceiptScanStep.Reviewing)
            assertEquals(1, reviewing.includedCount)

            viewModel.onEvent(ReceiptScanEvent.ToggleItemIncluded(reviewing.items[0].key))
            val toggled = awaitItem()
            assertEquals(0, toggled.includedCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `edit name re-runs matching for that item`() = runTest {
        coEvery { receiptTextRecognizer.recognizeText(any()) } returns "LECHE 1LT"
        coEvery { parseReceipt(any()) } returns listOf(scannedItem())
        coEvery { matchProducts(any()) } returnsMany listOf(
            listOf(ProductMatch(lecheItem, MatchConfidence.EXACT)),
            listOf(null),
        )

        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onEvent(ReceiptScanEvent.PhotoCaptured(byteArrayOf(1, 2, 3)))
            val reviewing = awaitStep(ReceiptScanStep.Reviewing)

            viewModel.onEvent(ReceiptScanEvent.EditItemName(reviewing.items[0].key, "Nuevo"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { matchProducts(any()) }
        coVerify { matchProducts(MatchProductsParams(listOf("Nuevo"))) }
    }

    @Test
    fun `toggle merge only when match exists`() = runTest {
        coEvery { receiptTextRecognizer.recognizeText(any()) } returns "LECHE ARROZ"
        coEvery { parseReceipt(any()) } returns listOf(scannedItem("Leche"), scannedItem("Arroz"))
        coEvery { matchProducts(any()) } returns listOf(
            ProductMatch(lecheItem, MatchConfidence.PARTIAL),
            null,
        )

        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onEvent(ReceiptScanEvent.PhotoCaptured(byteArrayOf(1, 2, 3)))
            val reviewing = awaitStep(ReceiptScanStep.Reviewing)
            val keyWithMatch = reviewing.items[0].key
            val keyWithoutMatch = reviewing.items[1].key

            viewModel.onEvent(ReceiptScanEvent.ToggleMerge(keyWithMatch))
            val afterToggle = awaitItem()
            assertEquals(
                lecheItem.id,
                afterToggle.items.first { it.key == keyWithMatch }.mergeIntoItemId,
            )

            viewModel.onEvent(ReceiptScanEvent.ToggleMerge(keyWithoutMatch))
            assertNull(viewModel.uiState.value.items.first { it.key == keyWithoutMatch }.mergeIntoItemId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirm with no included items sets error and does not call addFoodItems`() = runTest {
        coEvery { receiptTextRecognizer.recognizeText(any()) } returns "LECHE 1LT"
        coEvery { parseReceipt(any()) } returns listOf(scannedItem())
        coEvery { matchProducts(any()) } returns listOf(null)

        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onEvent(ReceiptScanEvent.PhotoCaptured(byteArrayOf(1, 2, 3)))
            val reviewing = awaitStep(ReceiptScanStep.Reviewing)

            viewModel.onEvent(ReceiptScanEvent.ToggleItemIncluded(reviewing.items[0].key))
            awaitItem()

            viewModel.onEvent(ReceiptScanEvent.ConfirmImport)
            val state = awaitItem()
            assertNotNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { addFoodItems(any()) }
    }

    @Test
    fun `confirm success emits Imported effect`() = runTest {
        coEvery { receiptTextRecognizer.recognizeText(any()) } returns "LECHE 1LT"
        coEvery { parseReceipt(any()) } returns listOf(scannedItem())
        coEvery { matchProducts(any()) } returns listOf(null)
        coEvery { addFoodItems(any()) } returns listOf(lecheItem)

        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(ReceiptScanEvent.PhotoCaptured(byteArrayOf(1, 2, 3)))
            viewModel.onEvent(ReceiptScanEvent.ConfirmImport)

            assertEquals(ReceiptScanEffect.Imported(1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<ReceiptScanUiState>.awaitStep(step: ReceiptScanStep): ReceiptScanUiState {
        while (true) {
            val state = awaitItem()
            if (state.step == step) return state
        }
    }

    private suspend fun ReceiveTurbine<ReceiptScanUiState>.awaitErrorState(): ReceiptScanUiState {
        while (true) {
            val state = awaitItem()
            if (state.error != null) return state
        }
    }

    private fun viewModel() = ReceiptScanViewModel(
        receiptTextRecognizer = receiptTextRecognizer,
        parseReceipt = parseReceipt,
        matchProducts = matchProducts,
        addFoodItems = addFoodItems,
    )
}
