package com.community.swaphub.data.repository

import android.content.Context
import android.net.Uri
import com.community.swaphub.data.api.ApiService
import com.community.swaphub.data.local.dao.ItemDao
import com.community.swaphub.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class ItemRepository @Inject constructor(
    private val apiService: ApiService,
    private val itemDao: ItemDao,
    private val context: Context
) {

    // ---------------------------------------------
    // 1️⃣ LOAD "OTHERS" ITEMS (Fix for Home screen)
    // ---------------------------------------------
    fun getAvailableItems(): Flow<Result<List<Item>>> = flow {
        try {
            val response = apiService.getOthersItems()

            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                    .map {
                        it.copy(
                            status = it.status ?: ItemStatus.AVAILABLE,
                            type = it.type ?: ItemType.SWAP
                        )
                    }
                    .filter { it.status == ItemStatus.AVAILABLE }

                itemDao.insertItems(list)
                emit(Result.success(list))
            } else {
                emit(Result.failure(Exception("Failed: ${response.message()}")))
            }

        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ---------------------------------------------
    // 2️⃣ LOAD SINGLE ITEM
    // ---------------------------------------------
    fun getItem(id: String): Flow<Result<Item>> = flow {
        try {
            val response = apiService.getItem(id)
            if (response.isSuccessful && response.body() != null) {
                val item = response.body()!!.copy(
                    status = response.body()!!.status ?: ItemStatus.AVAILABLE,
                    type = response.body()!!.type ?: ItemType.SWAP
                )
                itemDao.insertItem(item)
                emit(Result.success(item))
            } else {
                emit(Result.failure(Exception(response.message() ?: "Failed to fetch item")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // Add this function to ItemRepository.kt
    fun getNearbyItems(latitude: Double, longitude: Double, radiusKm: Double): Flow<Result<List<Item>>> = flow {
        try {
            val response = apiService.getNearbyItems(latitude, longitude, radiusKm)

            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                    .map {
                        it.copy(
                            status = it.status ?: ItemStatus.AVAILABLE,
                            type = it.type ?: ItemType.SWAP
                        )
                    }
                    .filter { it.status == ItemStatus.AVAILABLE }

                itemDao.insertItems(list)
                emit(Result.success(list))
            } else {
                emit(Result.failure(Exception("Failed: ${response.message()}")))
            }

        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // --------------------------------------------------------------------
    // 3️⃣ POST ITEM WITH IMAGE (Corrected with ContentResolver)
    // --------------------------------------------------------------------
    suspend fun postItemWithImage(
        title: String,
        description: String?,
        category: String?,
        type: ItemType,
        location: String?,
        latitude: Double?,
        longitude: Double?,
        imageUri: Uri?
    ): Result<Item> {
        return try {

            val text = "text/plain".toMediaType()

            val titlePart = RequestBody.create(text, title)
            val descPart = description?.let { RequestBody.create(text, it) }
            val categoryPart = category?.let { RequestBody.create(text, it) }
            val typePart = RequestBody.create(text, type.name)
            val locationPart = location?.let { RequestBody.create(text, it) }

            val imagePart: MultipartBody.Part? = imageUri?.let { uri ->

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return Result.failure(Exception("Cannot open image"))

                val bytes = inputStream.readBytes()
                inputStream.close()

                val fileBody = RequestBody.create("image/*".toMediaType(), bytes)

                MultipartBody.Part.createFormData(
                    "imageFile",
                    "uploaded_image.jpg",
                    fileBody
                )
            }

            val response = apiService.postItem(
                titlePart, descPart, categoryPart, typePart,
                locationPart, null, null, imagePart
            )

            if (response.isSuccessful && response.body() != null) {
                val saved = response.body()!!
                itemDao.insertItem(saved)
                Result.success(saved)
            } else {
                Result.failure(Exception(response.message()))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------
    // 4️⃣ DELETE ITEM
    // ---------------------------------------------
    suspend fun deleteItem(id: String): Result<Unit> {
        return try {
            val response = apiService.deleteItem(id)
            if (response.isSuccessful) {
                itemDao.getItemById(id).collect { item -> item?.let { itemDao.deleteItem(it) } }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------
    // 5️⃣ UPDATE ITEM (image included)
    // ---------------------------------------------
    suspend fun updateItem(
        itemId: String,
        title: String,
        description: String?,
        category: String?,
        type: ItemType,
        location: String?,
        imageUri: Uri?
    ): Flow<Result<Item>> = flow {
        try {
            val textPlain = "text/plain".toMediaType()

            val titlePart = RequestBody.create(textPlain, title)
            val descPart = description?.let { RequestBody.create(textPlain, it) }
            val categoryPart = category?.let { RequestBody.create(textPlain, it) }
            val typePart = RequestBody.create(textPlain, type.name)
            val locationPart = location?.let { RequestBody.create(textPlain, it) }

            val imagePart = imageUri?.let { uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                val req = RequestBody.create("image/*".toMediaType(), bytes!!)
                MultipartBody.Part.createFormData("imageFile", "updated.jpg", req)
            }

            val response = apiService.updateItem(
                id = itemId,
                title = titlePart,
                description = descPart,
                category = categoryPart,
                type = typePart,
                location = locationPart,
                latitude = null,
                longitude = null,
                imageFile = imagePart
            )

            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                itemDao.insertItem(updated)
                emit(Result.success(updated))
            } else {
                emit(Result.failure(Exception(response.message())))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ---------------------------------------------
    // 6️⃣ UPDATE ITEM STATUS
    // ---------------------------------------------
    suspend fun updateItemStatus(item: Item, newStatus: String): Result<Item> {
        return try {
            val typePlain = "text/plain".toMediaType()

            val titlePart = RequestBody.create(typePlain, item.title)
            val descPart = item.description?.let { RequestBody.create(typePlain, it) }
            val categoryPart = item.category?.let { RequestBody.create(typePlain, it) }
            val typePart = RequestBody.create(typePlain, item.type?.name ?: "SWAP")
            val locationPart = item.location?.let { RequestBody.create(typePlain, it) }
            val statusPart = RequestBody.create(typePlain, newStatus)

            val response = apiService.updateItemStatus(
                itemId = item.id,
                title = titlePart,
                description = descPart,
                category = categoryPart,
                type = typePart,
                location = locationPart,
                latitude = null,
                longitude = null,
                status = statusPart
            )

            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                itemDao.insertItem(updated)
                Result.success(updated)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------
    // 7️⃣ MY ITEMS
    // ---------------------------------------------
    fun getMyItems(): Flow<Result<List<Item>>> = flow {
        try {
            val response = apiService.getMyItems()
            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!
                    .map {
                        it.copy(
                            status = it.status ?: ItemStatus.AVAILABLE,
                            type = it.type ?: ItemType.SWAP
                        )
                    }

                itemDao.insertItems(items)
                emit(Result.success(items))
            } else {
                emit(Result.failure(Exception(response.message())))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getLocalItems(): Flow<List<Item>> = itemDao.getAllItems()
}
