package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName

data class HealthFactorResponse(
    @SerializedName("factorName")
    val factorName: String,

    @SerializedName("score")
    val score: Int,

    @SerializedName("maxScore")
    val maxScore: Int,

    @SerializedName("weightPercent")
    val weightPercent: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("detail")
    val detail: String
)

data class RoomHealthScoreResponse(
    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("roomCode")
    val roomCode: String,

    @SerializedName("propertyId")
    val propertyId: Long,

    @SerializedName("propertyName")
    val propertyName: String,

    @SerializedName("roomStatus")
    val roomStatus: RoomStatus,

    @SerializedName("tenantName")
    val tenantName: String?,

    @SerializedName("totalScore")
    val totalScore: Int,

    @SerializedName("ratingLevel")
    val ratingLevel: String,

    @SerializedName("ratingLabel")
    val ratingLabel: String,

    @SerializedName("trend")
    val trend: String,

    @SerializedName("scoreDelta")
    val scoreDelta: Int,

    @SerializedName("factors")
    val factors: List<HealthFactorResponse> = emptyList(),

    @SerializedName("reasonsForDeduction")
    val reasonsForDeduction: List<String> = emptyList(),

    @SerializedName("recommendations")
    val recommendations: List<String> = emptyList(),

    @SerializedName("formulaNote")
    val formulaNote: String?
)

data class HealthScoreOverviewResponse(
    @SerializedName("averageScore")
    val averageScore: Int,

    @SerializedName("totalRooms")
    val totalRooms: Int,

    @SerializedName("goodRoomsCount")
    val goodRoomsCount: Int,

    @SerializedName("warningRoomsCount")
    val warningRoomsCount: Int,

    @SerializedName("riskRoomsCount")
    val riskRoomsCount: Int,

    @SerializedName("formulaSummary")
    val formulaSummary: String?,

    @SerializedName("roomScores")
    val roomScores: List<RoomHealthScoreResponse> = emptyList()
)
