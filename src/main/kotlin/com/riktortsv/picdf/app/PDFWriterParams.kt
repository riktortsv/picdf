package com.riktortsv.picdf.app

import com.riktortsv.picdf.domain.PDFImageElement
import java.nio.file.Path

data class PDFWriterParams(val savePath: Path, val width: Double, val height: Double, val elements: List<PDFImageElement>)