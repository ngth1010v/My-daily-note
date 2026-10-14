package io.github.ngth1010v.mydailynote.feature.settings

internal data class ThirdParty(val name: String, val license: String, val url: String)

internal val thirdParties = listOf(
    ThirdParty("Kotlin, kotlinx.coroutines", "Apache-2.0", "https://github.com/JetBrains/kotlin"),
    ThirdParty("AndroidX (Compose, Room, Paging, DataStore, Lifecycle, Activity, AppCompat, Core)", "Apache-2.0", "https://developer.android.com/jetpack/androidx"),
    ThirdParty("Material Icons", "Apache-2.0", "https://fonts.google.com/icons"),
)

internal const val MIT_LICENSE = """MIT License

Copyright (c) 2026 ngth1010v

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."""
