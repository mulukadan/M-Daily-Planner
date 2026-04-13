package com.example.m_dailyplanner.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.example.m_dailyplanner.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val dataStoreManager: DataStoreManager = mockk(relaxed = true)
    private val application: Application = mockk(relaxed = true)
    
    private lateinit var viewModel: TaskViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { repository.getAllTasks() } returns flowOf(emptyList())
        every { dataStoreManager.carryForwardEvent } returns flowOf(null)
        every { dataStoreManager.showOnboarding } returns flowOf(false)
        
        viewModel = TaskViewModel(application, repository, dataStoreManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addTask should call repository insert`() = runTest {
        val task = Task(name = "Test Task", description = "", date = "2023-10-10", time = "", reminderEnabled = false, status = "PENDING")
        
        viewModel.addTask(task)
        advanceUntilIdle()
        
        coVerify { repository.insertTask(task) }
    }

    @Test
    fun `updateTask should call repository update`() = runTest {
        val task = Task(id = 1, name = "Test Task", description = "", date = "2023-10-10", time = "", reminderEnabled = false, status = "PENDING")
        val updatedTask = task.copy(status = "COMPLETED")
        
        viewModel.updateTask(updatedTask)
        advanceUntilIdle()
        
        coVerify { repository.updateTask(updatedTask) }
    }

    @Test
    fun `carryForwardTasks should call repository carryForward and clear DataStore`() = runTest {
        val oldDate = "2023-10-09"
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        viewModel.carryForwardTasks(oldDate)
        advanceUntilIdle()
        
        coVerify { repository.carryForwardTasks(oldDate, today) }
        coVerify { dataStoreManager.clearCarryForward() }
    }

    @Test
    fun `carryForwardEvent should emit correct data`() = runTest {
        val carryData = CarryForwardData(5, "2023-10-09")
        every { dataStoreManager.carryForwardEvent } returns flowOf(carryData)
        
        // Re-init viewModel to pick up the new flow
        viewModel = TaskViewModel(application, repository, dataStoreManager)
        
        viewModel.carryForwardEvent.test {
            assertEquals(carryData, awaitItem())
        }
    }

    @Test
    fun `setSelectedDate should update filteredTasks`() = runTest {
        val date1 = "2023-10-10"
        val date2 = "2023-10-11"
        val tasks = listOf(
            Task(id = 1, name = "Task 1", description = "", date = date1, time = "", reminderEnabled = false, status = "PENDING"),
            Task(id = 2, name = "Task 2", description = "", date = date2, time = "", reminderEnabled = false, status = "PENDING")
        )
        
        every { repository.getAllTasks() } returns flowOf(tasks)
        viewModel = TaskViewModel(application, repository, dataStoreManager)
        
        viewModel.setSelectedDate(date1)
        
        viewModel.filteredTasks.test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Task 1", items[0].name)
        }
    }
}
