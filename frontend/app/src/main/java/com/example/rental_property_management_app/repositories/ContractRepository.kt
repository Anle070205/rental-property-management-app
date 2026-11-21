package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContractRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getContracts(
        propertyId: Long? = null,
        roomId: Long? = null,
        tenantId: Long? = null,
        status: ContractStatus? = null
    ): Resource<List<ContractResponse>> = ApiResultHandler.execute {
        apiService.getContracts(propertyId, roomId, tenantId, status?.name)
    }

    suspend fun getContractById(id: Long): Resource<ContractResponse> = ApiResultHandler.execute {
        apiService.getContractById(id)
    }

    suspend fun createContract(request: CreateContractRequest): Resource<ContractResponse> =
        ApiResultHandler.execute {
            apiService.createContract(request)
        }

    suspend fun updateContract(id: Long, request: UpdateContractRequest): Resource<ContractResponse> =
        ApiResultHandler.execute {
            apiService.updateContract(id, request)
        }

    suspend fun signContract(id: Long): Resource<ContractResponse> =
        ApiResultHandler.execute {
            apiService.signContract(id)
        }

    suspend fun activateContract(id: Long): Resource<ContractResponse> =
        ApiResultHandler.execute {
            apiService.activateContract(id)
        }

    suspend fun terminateContract(id: Long, reason: String? = null): Resource<ContractResponse> =
        ApiResultHandler.execute {
            apiService.terminateContract(id, TerminateContractRequest(reason))
        }

    suspend fun getRoommates(
        tenantId: Long? = null,
        roomId: Long? = null,
        contractId: Long? = null
    ): Resource<List<RoommateResponse>> = ApiResultHandler.execute {
        apiService.getRoommates(tenantId, roomId, contractId)
    }

    suspend fun addRoommate(request: CreateRoommateRequest): Resource<RoommateResponse> =
        ApiResultHandler.execute {
            apiService.addRoommate(request)
        }

    suspend fun deleteRoommate(id: Long): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteRoommate(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (response.code() == 204 || body == null || body.success) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(body.message)
                }
            } else {
                Resource.Error(ApiResultHandler.extractErrorMessage(response))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
}
