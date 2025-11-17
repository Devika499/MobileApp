package com.community.swaphub.data.model

data class Notification(
    val id: String,
    val recipientId: String,
    val recipientName: String,
    val senderId: String?,
    val senderName: String?,
    val itemId: String?,
    val itemTitle: String?,
    val swapRequestId: String?,
    val type: String,
    val message: String,
    val read: Boolean,
    val createdAt: String
)