package com.foodario.scanning.domain.usecase

import com.foodario.catchError
import com.foodario.core.domain.matching.MatchConfidence
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ScanningUseCasesTest {

    private fun foodItem(
        id: Long = 1L,
        name: String = "Leche",
        quantity: Double = 1.0,
        unit: QuantityUnit = QuantityUnit.UNIT,
    ) = FoodItem(
        id = id,
        name = name,
        category = FoodCategory.DAIRY,
        quantity = quantity,
        unit = unit,
        isFrozen = false,
        expirationDate = null,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `MatchProductsUseCase returns parallel matches`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getAll() } returns listOf(
            foodItem(id = 1L, name = "Leche"),
            foodItem(id = 2L, name = "Leche condensada"),
        )
        val useCase = MatchProductsUseCase(repository)

        val result = useCase(MatchProductsParams(names = listOf("leche", "Yogur Bebible", "Arroz")))

        assertEquals(3, result.size)
        assertEquals(MatchConfidence.EXACT, result[0]?.confidence)
        assertEquals("Leche", result[0]?.item?.name)
        assertNull(result[1])
        assertNull(result[2])
    }

    @Test
    fun `AddFoodItemsUseCase rejects empty list`() = runTest {
        val repository = mockk<InventoryRepository>()
        val useCase = AddFoodItemsUseCase(repository)

        val error = catchError { useCase(AddFoodItemsParams(emptyList())) }

        assertIs<IllegalArgumentException>(error)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `AddFoodItemsUseCase inserts all when no matches`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.add(any()) } returnsMany listOf(
            foodItem(id = 100L, name = "Leche"),
            foodItem(id = 101L, name = "Arroz"),
        )
        val useCase = AddFoodItemsUseCase(repository)

        val result = useCase(
            AddFoodItemsParams(
                listOf(
                    AddFoodItemsParams.ImportedItem("Leche", FoodCategory.DAIRY, 1.0, QuantityUnit.UNIT),
                    AddFoodItemsParams.ImportedItem("Arroz", FoodCategory.PANTRY, 1.0, QuantityUnit.UNIT),
                ),
            ),
        )

        assertEquals(2, result.size)
        assertEquals(setOf(100L, 101L), result.map { it.id }.toSet())
        coVerify(exactly = 2) { repository.add(any()) }
        coVerify(exactly = 0) { repository.updateQuantity(any(), any()) }
    }

    @Test
    fun `AddFoodItemsUseCase merges exact matches within the same batch`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.add(any()) } returns foodItem(id = 1L, name = "Leche", quantity = 1.0)
        coEvery { repository.updateQuantity(any(), any()) } returns Unit
        val useCase = AddFoodItemsUseCase(repository)

        val result = useCase(
            AddFoodItemsParams(
                listOf(
                    AddFoodItemsParams.ImportedItem("Leche", FoodCategory.DAIRY, 1.0, QuantityUnit.UNIT),
                    AddFoodItemsParams.ImportedItem("Leche", FoodCategory.DAIRY, 1.0, QuantityUnit.UNIT),
                ),
            ),
        )

        assertEquals(2, result.size)
        coVerify(exactly = 1) { repository.add(any()) }
        coVerify(exactly = 1) { repository.updateQuantity(1L, 2.0) }
    }

    @Test
    fun `AddFoodItemsUseCase merges against existing inventory`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getAll() } returns listOf(foodItem(id = 5L, name = "Leche", quantity = 2.0))
        coEvery { repository.updateQuantity(any(), any()) } returns Unit
        val useCase = AddFoodItemsUseCase(repository)

        val result = useCase(
            AddFoodItemsParams(
                listOf(
                    AddFoodItemsParams.ImportedItem("LECHE", FoodCategory.DAIRY, 1.0, QuantityUnit.UNIT),
                ),
            ),
        )

        assertEquals(3.0, result[0].quantity)
        coVerify(exactly = 1) { repository.updateQuantity(5L, 3.0) }
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `explicit mergeIntoItemId wins over partial auto match`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getAll() } returns listOf(foodItem(id = 7L, name = "Leche", quantity = 1.0))
        coEvery { repository.updateQuantity(any(), any()) } returns Unit
        val useCase = AddFoodItemsUseCase(repository)

        useCase(
            AddFoodItemsParams(
                listOf(
                    AddFoodItemsParams.ImportedItem(
                        "Leche Entera",
                        FoodCategory.DAIRY,
                        1.0,
                        QuantityUnit.UNIT,
                        mergeIntoItemId = 7L,
                    ),
                ),
            ),
        )

        coVerify(exactly = 1) { repository.updateQuantity(7L, 2.0) }
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `explicit mergeIntoItemId with different unit inserts instead`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getAll() } returns listOf(
            foodItem(id = 7L, name = "Leche", quantity = 1.0, unit = QuantityUnit.GRAMS),
        )
        coEvery { repository.add(any()) } returns foodItem(id = 50L, name = "Leche", quantity = 1.0)
        val useCase = AddFoodItemsUseCase(repository)

        useCase(
            AddFoodItemsParams(
                listOf(
                    AddFoodItemsParams.ImportedItem(
                        "Leche",
                        FoodCategory.DAIRY,
                        1.0,
                        QuantityUnit.UNIT,
                        mergeIntoItemId = 7L,
                    ),
                ),
            ),
        )

        coVerify(exactly = 1) { repository.add(any()) }
        coVerify(exactly = 0) { repository.updateQuantity(any(), any()) }
    }
}
