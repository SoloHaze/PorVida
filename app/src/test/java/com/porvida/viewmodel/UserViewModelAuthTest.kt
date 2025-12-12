package com.porvida.viewmodel

import com.porvida.data.repos.UserRepository
import com.porvida.models.User
import com.porvida.models.UsuarioDao
import com.porvida.views.UserViewModel
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private class FakeUsuarioDaoVM : UsuarioDao {
    private val users = mutableListOf<User>()
    private val flow = MutableStateFlow<List<User>>(emptyList())
    override fun getAllUsers(): Flow<List<User>> = flow
    override suspend fun findByEmail(email: String): User? = users.firstOrNull { it.email == email }
    override suspend fun getUserById(userId: String): User? = users.firstOrNull { it.id == userId }
    override suspend fun insertUser(user: User) {
        users.removeIf { it.id == user.id }
        users.add(user)
        flow.value = users.toList()
    }
    override suspend fun deleteUser(user: User) {
        users.removeIf { it.id == user.id }
        flow.value = users.toList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelAuthTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    @Test
    @DisplayName("UserViewModel: registro seguido de login actualiza estado a Success")
    fun registrarLuegoLoginActualizaEstado() = runTest {
        Dispatchers.setMain(testDispatcher)
        val dao = FakeUsuarioDaoVM()
        val repo = UserRepository(dao)
        val vm = UserViewModel(repo)

        // Registrar
        vm.register(name = "Carlos", email = "carlos@example.com", password = "superPass", plan = "ULTRA")
        advanceUntilIdle()
        vm.loginState.value shouldBe UserViewModel.LoginState.RegisterSuccess

        // Login
        vm.login("carlos@example.com", "superPass")
        advanceUntilIdle()
        val state = vm.loginState.value
        (state is UserViewModel.LoginState.Success) shouldBe true
        val user = (state as UserViewModel.LoginState.Success).user
        user.email shouldBe "carlos@example.com"
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("UserViewModel: login falla con password incorrecta retorna Error")
    fun loginFallaConPasswordIncorrecta() = runTest {
        Dispatchers.setMain(testDispatcher)
        val dao = FakeUsuarioDaoVM()
        val repo = UserRepository(dao)
        val vm = UserViewModel(repo)

        vm.register(name = "Ana", email = "ana@example.com", password = "claveAna", plan = "BASICO")
        advanceUntilIdle()
        vm.login("ana@example.com", "otraClave")
        advanceUntilIdle()
        val state = vm.loginState.value
        (state is UserViewModel.LoginState.Error) shouldBe true
        Dispatchers.resetMain()
    }
}
