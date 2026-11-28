package dev.aicon.studio.model

/**
 * One downloadable model in the catalog. Every download starts from a user
 * tap on the Models screen (network policy); nothing here phones home.
 */
data class ModelInfo(
    val id: String,
    val title: String,
    val summary: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadUrl: String,
    val fileName: String,
    val templateAsset: String?,
    val license: String,
    val sourceUrl: String,
)

object ModelCatalog {

    val models: List<ModelInfo> = listOf(
        ModelInfo(
            id = "wepm-2b-q4km",
            title = "WePM 2B (Q4_K_M)",
            summary = "Tencent WeChat AI on-device model. Text chat in this build. 1.27 GB download.",
            sizeBytes = 1_274_395_392L,
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
            summary = "Bigger WePM variant for phones with room to spare. Text chat in this build. 2.71 GB download.",
            sizeBytes = 2_708_803_200L,
            sha256 = "5fec84c076a127308a6f0993997744b953f1156c592f9f10c69cb21632636d14",
            downloadUrl = "https://huggingface.co/tencent/WePocketModel1.0-4B-GGUF/resolve/main/language-Q4_K_M.gguf",
            fileName = "language-Q4_K_M.gguf",
            templateAsset = "models/wepm-chat-template.jinja",
            license = "Apache-2.0",
            sourceUrl = "https://huggingface.co/tencent/WePocketModel1.0-4B-GGUF",
        ),
    )

    fun byId(id: String): ModelInfo? = models.firstOrNull { it.id == id }
}
