package com.community.swaphub.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    @SerializedName("type") val type: ItemType? = ItemType.SWAP, // make safe
    @SerializedName("imageUrl") val imageUrl: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    // ✅ Backend doesn't send this field — so we allow null and default it safely in code
    @SerializedName("status") val status: ItemStatus? = ItemStatus.AVAILABLE,

    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("ownerId") val ownerId: String? = null,
    val ownerName: String? = null
)

enum class ItemType {
    GIVEAWAY,
    SWAP
}

enum class ItemStatus {
    AVAILABLE,
    SWAPPED,
    PENDING_SWAP,
    DELETED
}

data class PostItemRequest(
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val type: ItemType,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
