package io.github.ngth1010v.mydailynote.speech.api

enum class ModelKind { VOSK, WHISPER }

/**
 * A downloadable speech model. [archive] models are zips unpacked into a folder (Vosk);
 * others are a single file (Whisper ggml). [sha256] is checked after every download.
 */
data class ModelSpec(
    val id: String,
    val kind: ModelKind,
    /** Null = multilingual. */
    val language: VoiceLanguage?,
    val displayName: String,
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
    val archive: Boolean,
    val license: String,
)

object ModelRegistry {
    val all: List<ModelSpec> = listOf(
        ModelSpec(
            id = "vosk-small-en-us-0.15",
            kind = ModelKind.VOSK,
            language = VoiceLanguage.EN,
            displayName = "Vosk small English (US) 0.15",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
            sha256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498",
            sizeBytes = 41_205_931,
            archive = true,
            license = "Apache-2.0",
        ),
        ModelSpec(
            id = "vosk-en-us-0.22-lgraph",
            kind = ModelKind.VOSK,
            language = VoiceLanguage.EN,
            displayName = "Vosk English (US) 0.22 lgraph (more accurate)",
            url = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-lgraph.zip",
            sha256 = "d9838b4aaa82a75c4a17f5aca300eaca129aaab2a7cbf951bafbb500eb9c4334",
            sizeBytes = 130_557_655,
            archive = true,
            license = "Apache-2.0",
        ),
        ModelSpec(
            id = "vosk-small-vn-0.4",
            kind = ModelKind.VOSK,
            language = VoiceLanguage.VI,
            displayName = "Vosk small Vietnamese 0.4",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-vn-0.4.zip",
            sha256 = "efe5c8494212110471a79befc48c79da679e5b1fc52a4ffb500222ff86d622e5",
            sizeBytes = 33_656_337,
            archive = true,
            license = "Apache-2.0",
        ),
        ModelSpec(
            id = "whisper-tiny-q5_1",
            kind = ModelKind.WHISPER,
            language = null,
            displayName = "Whisper tiny (multilingual, q5_1)",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin",
            sha256 = "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7",
            sizeBytes = 32_152_673,
            archive = false,
            license = "MIT",
        ),
        ModelSpec(
            id = "whisper-base-q5_1",
            kind = ModelKind.WHISPER,
            language = null,
            displayName = "Whisper base (multilingual, q5_1)",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
            sha256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
            sizeBytes = 59_707_625,
            archive = false,
            license = "MIT",
        ),
        ModelSpec(
            id = "whisper-small-q5_1",
            kind = ModelKind.WHISPER,
            language = null,
            displayName = "Whisper small (multilingual, q5_1)",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin",
            sha256 = "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
            sizeBytes = 190_085_487,
            archive = false,
            license = "MIT",
        ),
        ModelSpec(
            id = "whisper-medium-q5_0",
            kind = ModelKind.WHISPER,
            language = null,
            displayName = "Whisper medium (multilingual, q5_0)",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q5_0.bin",
            sha256 = "19fea4b380c3a618ec4723c3eef2eb785ffba0d0538cf43f8f235e7b3b34220f",
            sizeBytes = 539_212_467,
            archive = false,
            license = "MIT",
        ),
        ModelSpec(
            id = "whisper-large-v3-turbo-q5_0",
            kind = ModelKind.WHISPER,
            language = null,
            displayName = "Whisper large-v3 turbo (multilingual, q5_0)",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q5_0.bin",
            sha256 = "394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2",
            sizeBytes = 574_041_195,
            archive = false,
            license = "MIT",
        ),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): ModelSpec? = byId[id]

    /** The default (small) Vosk model for [language]: the one offered for download first. */
    fun vosk(language: VoiceLanguage): ModelSpec =
        all.first { it.kind == ModelKind.VOSK && it.language == language }

    /** Vosk models for [language], most accurate first. */
    fun voskByAccuracy(language: VoiceLanguage): List<ModelSpec> =
        all.filter { it.kind == ModelKind.VOSK && it.language == language }.sortedByDescending { it.sizeBytes }

    val whisper: List<ModelSpec> get() = all.filter { it.kind == ModelKind.WHISPER }
}
