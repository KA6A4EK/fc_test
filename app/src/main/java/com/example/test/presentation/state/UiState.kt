package com.example.test.presentation.state

data class UiState(
    val latency: Long? = null,
    val cidResult: String = "",
    val loading: Boolean = false,
    /** Error from fetch CID (invalid CID, block not found, network, etc.). */
    val cidError: String? = null,
    /** Error from ping (node unreachable, timeout). */
    val pingError: String? = null,
)
