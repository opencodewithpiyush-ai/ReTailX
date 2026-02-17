package com.rajatt7z.retailx.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.rajatt7z.retailx.repository.AuthRepository
import com.rajatt7z.retailx.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: AuthRepository

    @Mock
    private lateinit var authStatusObserver: Observer<Resource<String>>

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(repository)
        viewModel.authStatus.observeForever(authStatusObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.authStatus.removeObserver(authStatusObserver)
    }

    @Test
    fun `loginUser should emit Loading then Success when repository returns Success`() = runTest(testDispatcher) {
        // Given
        val email = "test@example.com"
        val password = "password"
        `when`(repository.loginUser(email, password)).thenReturn(Resource.Success("Login Successful"))

        // When
        viewModel.loginUser(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(repository).loginUser(email, password)
        verify(authStatusObserver).onChanged(Resource.Loading())
        verify(authStatusObserver).onChanged(Resource.Success("Login Successful"))
    }

    @Test
    fun `loginUser should emit Loading then Error when repository returns Error`() = runTest(testDispatcher) {
        // Given
        val email = "test@example.com"
        val password = "wrong_password"
        val errorMessage = "Login Failed"
        `when`(repository.loginUser(email, password)).thenReturn(Resource.Error(errorMessage))

        // When
        viewModel.loginUser(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(repository).loginUser(email, password)
        verify(authStatusObserver).onChanged(Resource.Loading())
        verify(authStatusObserver).onChanged(Resource.Error(errorMessage))
    }
}
