package com.riktortsv.picdf.controller

import com.riktortsv.picdf.domain.PDFImageElement

interface MainPresenter {

    suspend fun proceed(element: PDFImageElement)

    suspend fun failure(element: PDFImageElement, cause: String)

    suspend fun confirm(title: String, context: String): Boolean

    suspend fun message(title: String, context: String)

    suspend fun error(title: String, context: String)

    suspend fun succeed()

    suspend fun cancel()

}