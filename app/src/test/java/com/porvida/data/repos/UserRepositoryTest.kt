package com.porvida.data.repos

import at.favre.lib.crypto.bcrypt.BCrypt
import com.porvida.models.User
import com.porvida.models.UsuarioDao
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private class FakeUsuarioDao : UsuarioDao {
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

class UserRepositoryTest {

    @Test
    @DisplayName("UserRepository: registrar usuario genera hash válido y retorna true")
    fun registrarUsuarioHashValido() = runTest {
        val dao = FakeUsuarioDao()
        val repo = UserRepository(dao)
        val ok = repo.addUser(
            name = "Juan Perez",
            email = "juan@example.com",
            password = "claveSegura",
            plan = "BASICO"
        )
        ok shouldBe true
        val stored = dao.findByEmail("juan@example.com")
        stored shouldNotBe null
        stored!!.password shouldNotBe "claveSegura"
        val verify = BCrypt.verifyer().verify("claveSegura".toCharArray(), stored.password)
        verify.verified shouldBe true
        stored.plan shouldBe "BASICO"
    }

    @Test
    @DisplayName("UserRepository: login correcto retorna usuario; password incorrecta null")
    fun loginCorrectoEIncorrecto() = runTest {
        val dao = FakeUsuarioDao()
        val repo = UserRepository(dao)
        repo.addUser(
            name = "Maria",
            email = "maria@example.com",
            password = "pass123",
            plan = "BLACK"
        ) shouldBe true
        val success = repo.loginUser("maria@example.com", "pass123")
        success!!.email shouldBe "maria@example.com"
        val fail = repo.loginUser("maria@example.com", "otra")
        fail shouldBe null
    }
}
