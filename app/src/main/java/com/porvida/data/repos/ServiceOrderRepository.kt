package com.porvida.data.repos

import com.porvida.models.ServiceOrder
import com.porvida.models.ServiceOrderDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ServiceOrderRepository(private val serviceOrderDao: ServiceOrderDao) {
    
    fun getOrdersByUserId(userId: String): Flow<List<ServiceOrder>> = 
        serviceOrderDao.getOrdersByUserId(userId)
    
    fun getAllOrders(): Flow<List<ServiceOrder>> = serviceOrderDao.getAllOrders()
    
    suspend fun getOrderById(orderId: String): ServiceOrder? = 
        serviceOrderDao.getOrderById(orderId)
    
    suspend fun createOrder(
        userId: String,
        serviceId: String,
        description: String,
        scheduledDate: Long,
        totalAmount: Double
    ): Boolean {
        return try {
            val order = ServiceOrder(
                id = UUID.randomUUID().toString(),
                userId = userId,
                serviceId = serviceId,
                status = "PENDING",
                description = description,
                scheduledDate = scheduledDate,
                totalAmount = totalAmount
            )
            serviceOrderDao.insertOrder(order)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateOrderStatus(orderId: String, status: String) {
        serviceOrderDao.updateOrderStatus(orderId, status)
    }
    
    suspend fun deleteOrder(order: ServiceOrder) {
        serviceOrderDao.deleteOrder(order)
    }
}