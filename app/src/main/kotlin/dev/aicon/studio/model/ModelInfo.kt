package dev.aicon.studio.model

/** The catalog tab a model belongs to. */
enum class ModelCategory(val label: String) {
    CHAT("Chat"),
    VISION("Vision"),
    TRANSLATION("Translation"),
    SPEECH_TO_TEXT("Speech to text"),
    TEXT_TO_SPEECH("Text to speech"),
    IMAGE("Image"),
    EMBEDDINGS("Embeddings"),
    VIDEO("Video"),
}

/** Honest state of a catalog entry in this build. */
enum class Readiness(val label: String) {
    READY("Ready"),
    NEEDS_RUNTIME("Needs runtime work"),
    NOT_YET("Not yet"),
}

/** The vision adapter (mmproj) a multimodal model needs next to its GGUF. */
data class MmprojInfo(
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadUrl: String,
)

/**
 * One catalog entry. READY entries carry a download URL and a SHA-256 that
 * the download flow verifies byte for byte; NEEDS_RUNTIME and NOT_YET
 * entries describe what is coming and offer no download. Every download
 * starts from a user tap on the Models screen (network policy); nothing
 * here phones home.
 */
data class ModelInfo(
    val id: String,
    val title: String,
    val summary: String,
    val category: ModelCategory,
    val readiness: Readiness,
    val sizeBytes: Long,
    val ramMb: Int?,
    val sha256: String?,
    val downloadUrl: String?,
    val fileName: String,
    val templateAsset: String?,
    val license: String,
    val sourceUrl: String,
    val mmproj: MmprojInfo? = null,
) {
    val downloadable: Boolean
        get() = readiness == Readiness.READY && downloadUrl != null && sha256 != null
}

object ModelCatalog {

    val models: List<ModelInfo> = listOf(
        // --- Chat -----------------------------------------------------
        ModelInfo(
            id = "wepm-2b-q4km",
            title = "WePM 2B (Q4_K_M)",
            summary = "Tencent WeChat AI on-device model for text chat. 1.27 GB download.",
            category = ModelCategory.CHAT,
            readiness = Readiness.READY,
            sizeBytes = 1_274_395_392L,
            ramMb = 2500,
            sha256 = "3d6cdcdbc5141e70616867f62146203fb47dab6fe9394914aff9e1bc89094f1a",
            downloadUrl = "https://huggingface.co/tencent/WePocketModel1.0-2B-GGUF/resolve/main/language-Q4_K_M.gguf",
            fileName = "language-Q4_K_M.gguf",
            templateAsset = "models/wepm-chat-template.jinja",
            license = "Apache-2.0",
            sourceUrl = "https://huggingface.co/tencent/WePocketModel1.0-2B-GGUF",
        ),
        ModelInfo(
            id = "wepm-4b-q4km",
            title = "WePM 4B (Q4_K_M)",
            summary = "Bigger WePM variant for phones with room to spare. 2.71 GB download.",
            category = ModelCategory.CHAT,
            readiness = Readiness.READY,
            sizeBytes = 2_708_803_200L,
            ramMb = 4200,
            sha256 = "5fec84c076a127308a6f0993997744b953f1156c592f9f10c69cb21632636d14",
            downloadUrl = "https://huggingface.co/tencent/WePocketModel1.0-4B-GGUF/resolve/main/language-Q4_K_M.gguf",
            fileName = "language-Q4_K_M.gguf",
            templateAsset = "models/wepm-chat-template.jinja",
            license = "Apache-2.0",
            sourceUrl = "https://huggingface.co/tencent/WePocketModel1.0-4B-GGUF",
        ),
        // --- Vision ---------------------------------------------------
        ModelInfo(
            id = "smolvlm2-2.2b-q4km",
            title = "SmolVLM2 2.2B (Q4_K_M)",
            summary = "Sees a photo you attach in chat and answers questions about it. " +
                "1.11 GB plus a 593 MB vision adapter.",
            category = ModelCategory.VISION,
            readiness = Readiness.READY,
            sizeBytes = 1_112_602_656L,
            ramMb = 3000,
            sha256 = "0cf76814555b8665149075b74ab6b5c1d428ea1d3d01c1918c12012e8d7c9f58",
            downloadUrl = "https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/SmolVLM2-2.2B-Instruct-Q4_K_M.gguf",
            fileName = "SmolVLM2-2.2B-Instruct-Q4_K_M.gguf",
            templateAsset = null,
            license = "Apache-2.0",
            sourceUrl = "https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF",
            mmproj = MmprojInfo(
                fileName = "mmproj-SmolVLM2-2.2B-Instruct-Q8_0.gguf",
                sizeBytes = 592_523_200L,
                sha256 = "ae07ea1facd07dd3230c4483b63e8cda96c6944ad2481f33d531f79e892dd024",
                downloadUrl = "https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-2.2B-Instruct-Q8_0.gguf",
            ),
        ),
        ModelInfo(
            id = "smolvlm2-500m-q8",
            title = "SmolVLM2 500M (Q8_0)",
            summary = "Lighter vision model for phones with less room. " +
                "437 MB plus a 109 MB vision adapter.",
            category = ModelCategory.VISION,
            readiness = Readiness.READY,
            sizeBytes = 436_808_704L,
            ramMb = 1200,
            sha256 = "6f67b8036b2469fcd71728702720c6b51aebd759b78137a8120733b4d66438bc",
            downloadUrl = "https://huggingface.co/ggml-org/SmolVLM2-500M-Video-Instruct-GGUF/resolve/main/SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
            fileName = "SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
            templateAsset = null,
            license = "Apache-2.0",
            sourceUrl = "https://huggingface.co/ggml-org/SmolVLM2-500M-Video-Instruct-GGUF",
            mmproj = MmprojInfo(
                fileName = "mmproj-SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
                sizeBytes = 108_785_184L,
                sha256 = "921dc7e259f308e5b027111fa185efcbf33db13f6e35749ddf7f5cdb60ef520b",
                downloadUrl = "https://huggingface.co/ggml-org/SmolVLM2-500M-Video-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
            ),
        ),
        // --- Translation ------------------------------------------------
        ModelInfo(
            id = "hymt3-1.8b-q4km",
            title = "Hy-MT3 1.8B (Q4_K_M)",
            summary = "Tencent translation model covering 102 languages, Malayalam included. " +
                "Runs on the same engine as chat. 1.35 GB download.",
            category = ModelCategory.TRANSLATION,
            readiness = Readiness.READY,
            sizeBytes = 1_348_432_160L,
            ramMb = 2200,
            sha256 = "bae6e591103654ff43ef0de7d1492f6e6df1f955505f6bb2cc52e86fc399a942",
            downloadUrl = "https://huggingface.co/tencent/Hy-MT3-1.8B-GGUF/resolve/main/Hy-MT3-1.8B-Q4_K_M.gguf",
            fileName = "Hy-MT3-1.8B-Q4_K_M.gguf",
            templateAsset = null,
            license = "Apache-2.0",
            sourceUrl = "https://huggingface.co/tencent/Hy-MT3-1.8B-GGUF",
        ),
        // --- Speech to text -------------------------------------------
        ModelInfo(
            id = "whisper-base",
            title = "Whisper base (whisper.cpp)",
            summary = "On-device speech to text. Runs through whisper.cpp (MIT), " +
                "which is not wired into this build yet, so there is nothing to download.",
            category = ModelCategory.SPEECH_TO_TEXT,
            readiness = Readiness.NEEDS_RUNTIME,
            sizeBytes = 145_000_000L,
            ramMb = 500,
            sha256 = null,
            downloadUrl = null,
            fileName = "",
            templateAsset = null,
            license = "MIT",
            sourceUrl = "https://github.com/ggml-org/whisper.cpp",
        ),
        // --- Text to speech -------------------------------------------
        ModelInfo(
            id = "kokoro-82m",
            title = "Kokoro 82M (sherpa-onnx)",
            summary = "Natural on-device voices. Runs through sherpa-onnx (Apache-2.0), " +
                "planned for a later build.",
            category = ModelCategory.TEXT_TO_SPEECH,
            readiness = Readiness.NEEDS_RUNTIME,
            sizeBytes = 350_000_000L,
            ramMb = 600,
            sha256 = null,
            downloadUrl = null,
            fileName = "",
            templateAsset = null,
            license = "Apache-2.0",
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx",
        ),
        // --- Image generation -----------------------------------------
        ModelInfo(
            id = "sd15-stable-diffusion-cpp",
            title = "Stable Diffusion 1.5 (stable-diffusion.cpp)",
            summary = "On-device image generation. Works on flagship phones and is slow " +
                "elsewhere. Needs the stable-diffusion.cpp (MIT) runtime in a later build.",
            category = ModelCategory.IMAGE,
            readiness = Readiness.NEEDS_RUNTIME,
            sizeBytes = 2_000_000_000L,
            ramMb = 4000,
            sha256 = null,
            downloadUrl = null,
            fileName = "",
            templateAsset = null,
            license = "MIT runtime; model terms vary",
            sourceUrl = "https://github.com/leejet/stable-diffusion.cpp",
        ),
        // --- Embeddings -----------------------------------------------
        ModelInfo(
            id = "bge-small-en",
            title = "bge-small-en (GGUF)",
            summary = "Text embeddings for on-device search and categorization. " +
                "The current engine supports them; the wiring lands with the WePM bundle step.",
            category = ModelCategory.EMBEDDINGS,
            readiness = Readiness.NEEDS_RUNTIME,
            sizeBytes = 130_000_000L,
            ramMb = 400,
            sha256 = null,
            downloadUrl = null,
            fileName = "",
            templateAsset = null,
            license = "MIT",
            sourceUrl = "https://huggingface.co/BAAI/bge-small-en-v1.5",
        ),
        // --- Video ----------------------------------------------------
        ModelInfo(
            id = "video-placeholder",
            title = "Video understanding",
            summary = "Not realistic on a phone today. This entry keeps the catalog " +
                "honest about what is not here yet.",
            category = ModelCategory.VIDEO,
            readiness = Readiness.NOT_YET,
            sizeBytes = 0L,
            ramMb = null,
            sha256 = null,
            downloadUrl = null,
            fileName = "",
            templateAsset = null,
            license = "n/a",
            sourceUrl = "https://github.com/aicon-hub/aicon-ai-studio",
        ),
    )

    fun byId(id: String): ModelInfo? = models.firstOrNull { it.id == id }

    fun byCategory(category: ModelCategory): List<ModelInfo> =
        models.filter { it.category == category }
}
