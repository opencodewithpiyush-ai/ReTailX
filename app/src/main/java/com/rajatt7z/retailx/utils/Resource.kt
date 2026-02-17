package com.rajatt7z.retailx.utils

sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data) {
        override fun equals(other: Any?): Boolean = other is Success<*> && other.data == data
        override fun hashCode(): Int = data?.hashCode() ?: 0
    }

    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message) {
        override fun equals(other: Any?): Boolean = other is Error<*> && other.message == message && other.data == data
        override fun hashCode(): Int = 31 * message.hashCode() + (data?.hashCode() ?: 0)
    }

    class Loading<T> : Resource<T>() {
        override fun equals(other: Any?): Boolean = other is Loading<*>
        override fun hashCode(): Int = javaClass.hashCode()
    }
}
