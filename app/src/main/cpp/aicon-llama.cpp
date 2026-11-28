// JNI bridge between LlamaCppEngine (Kotlin) and llama.cpp.
// Completion-loop logic adapted from llama.cpp's examples/llama.android
// ai_chat.cpp and the image flow from tools/mtmd/mtmd-cli.cpp (both MIT
// license, (c) the ggml authors), renamed and trimmed to what AiCoN AI
// Studio's chat screen needs.
#include <android/log.h>
#include <jni.h>
#include <string>
#include <sstream>
#include <vector>
#include <unistd.h>
#include <sampling.h>

#include "aicon-log.h"
#include "chat.h"
#include "common.h"
#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

constexpr int   N_THREADS_MIN           = 2;
constexpr int   N_THREADS_MAX           = 4;
constexpr int   N_THREADS_HEADROOM      = 2;

constexpr int   DEFAULT_CONTEXT_SIZE    = 4096;
constexpr int   OVERFLOW_HEADROOM       = 4;
constexpr int   BATCH_SIZE              = 512;
constexpr float DEFAULT_SAMPLER_TEMP    = 0.7f;

static llama_model                 * g_model;
static llama_context               * g_context;
static common_batch                  g_batch;
static common_chat_templates_ptr     g_chat_templates;
static common_sampler              * g_sampler;
static std::string                   g_chat_template_override;
static mtmd_context                * g_mtmd = nullptr;

static int best_thread_count() {
    return std::max(N_THREADS_MIN, std::min(N_THREADS_MAX,
                        (int) sysconf(_SC_NPROCESSORS_ONLN) - N_THREADS_HEADROOM));
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeInit(JNIEnv *env, jobject /*unused*/, jstring nativeLibDir) {
    llama_log_set(aicon_android_log_callback, nullptr);

    const auto *path_to_backend = env->GetStringUTFChars(nativeLibDir, 0);
    LOGi("Loading backends from %s", path_to_backend);
    ggml_backend_load_all_from_path(path_to_backend);
    env->ReleaseStringUTFChars(nativeLibDir, path_to_backend);

    llama_backend_init();
    LOGi("Backend initiated; log handler set.");
}

extern "C"
JNIEXPORT jint JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeLoadModel(JNIEnv *env, jobject, jstring jmodel_path) {
    llama_model_params model_params = llama_model_default_params();

    const auto *model_path = env->GetStringUTFChars(jmodel_path, 0);
    LOGd("%s: loading model from: %s", __func__, model_path);

    auto *model = llama_model_load_from_file(model_path, model_params);
    env->ReleaseStringUTFChars(jmodel_path, model_path);
    if (!model) {
        return 1;
    }
    g_model = model;
    return 0;
}

static llama_context *init_context(llama_model *model, const int n_ctx = DEFAULT_CONTEXT_SIZE) {
    if (!model) {
        LOGe("%s: model cannot be null", __func__);
        return nullptr;
    }

    const int n_threads = best_thread_count();
    LOGi("%s: using %d threads", __func__, n_threads);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = n_ctx;
    ctx_params.n_batch = BATCH_SIZE;
    ctx_params.n_ubatch = BATCH_SIZE;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    auto *context = llama_init_from_model(g_model, ctx_params);
    if (context == nullptr) {
        LOGe("%s: llama_init_from_model() returned null", __func__);
    }
    return context;
}

extern "C"
JNIEXPORT jint JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativePrepare(JNIEnv * /*env*/, jobject /*unused*/) {
    auto *context = init_context(g_model);
    if (!context) { return 1; }
    g_context = context;
    g_batch = common_batch(context);
    g_chat_templates = common_chat_templates_init(g_model, g_chat_template_override);
    common_params_sampling sparams;
    sparams.temp = DEFAULT_SAMPLER_TEMP;
    g_sampler = common_sampler_init(g_model, sparams);
    return 0;
}

// Load the mmproj vision adapter for multimodal models (SmolVLM2 and
// friends). Requires the text model to be loaded first.
// Returns 0 on success, 1 when no model is loaded, 2 when the adapter file
// failed to load, 3 when it exposes no vision capability.
extern "C"
JNIEXPORT jint JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeLoadMmproj(JNIEnv *env, jobject, jstring jmmproj_path) {
    if (!g_model) {
        return 1;
    }
    mtmd_context_params mparams = mtmd_context_params_default();
    mparams.use_gpu = false;
    mparams.print_timings = false;
    mparams.n_threads = best_thread_count();
    mparams.media_marker = mtmd_default_marker();

    const auto *mmproj_path = env->GetStringUTFChars(jmmproj_path, 0);
    LOGd("%s: loading mmproj from: %s", __func__, mmproj_path);
    auto *mctx = mtmd_init_from_file(mmproj_path, g_model, mparams);
    env->ReleaseStringUTFChars(jmmproj_path, mmproj_path);
    if (!mctx) {
        return 2;
    }
    if (!mtmd_support_vision(mctx)) {
        mtmd_free(mctx);
        return 3;
    }
    g_mtmd = mctx;
    LOGi("mmproj loaded; vision ready.");
    return 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeHasVision(JNIEnv * /*env*/, jobject /*unused*/) {
    return (g_mtmd != nullptr && mtmd_support_vision(g_mtmd)) ? JNI_TRUE : JNI_FALSE;
}

// Completion loop state
constexpr const char *ROLE_SYSTEM    = "system";
constexpr const char *ROLE_USER      = "user";
constexpr const char *ROLE_ASSISTANT = "assistant";

static std::vector<common_chat_msg> chat_msgs;
static llama_pos system_prompt_position;
static llama_pos current_position;

static void reset_long_term_states(const bool clear_kv_cache = true) {
    chat_msgs.clear();
    system_prompt_position = 0;
    current_position = 0;
    if (clear_kv_cache)
        llama_memory_clear(llama_get_memory(g_context), false);
}

static void shift_context() {
    const int n_discard = (current_position - system_prompt_position) / 2;
    LOGi("%s: discarding %d tokens", __func__, n_discard);
    llama_memory_seq_rm(llama_get_memory(g_context), 0, system_prompt_position, system_prompt_position + n_discard);
    llama_memory_seq_add(llama_get_memory(g_context), 0, system_prompt_position + n_discard, current_position, -n_discard);
    current_position -= n_discard;
}

static std::string chat_add_and_format(const std::string &role, const std::string &content) {
    common_chat_msg new_msg;
    new_msg.role = role;
    new_msg.content = content;
    auto formatted = common_chat_format_single(
            g_chat_templates.get(), chat_msgs, new_msg, role == ROLE_USER, /* use_jinja */ true);
    chat_msgs.push_back(new_msg);
    return formatted;
}

static llama_pos stop_generation_position;
static std::string cached_token_chars;
static std::ostringstream assistant_ss;

static void reset_short_term_states() {
    stop_generation_position = 0;
    cached_token_chars.clear();
    assistant_ss.str("");
}

static int decode_tokens_in_batches(
        llama_context *context,
        common_batch &batch,
        const llama_tokens &tokens,
        const llama_pos start_pos,
        const bool compute_last_logit = false) {
    for (int i = 0; i < (int) tokens.size(); i += BATCH_SIZE) {
        const int cur_batch_size = std::min((int) tokens.size() - i, BATCH_SIZE);
        batch.clear();

        if (start_pos + i + cur_batch_size >= DEFAULT_CONTEXT_SIZE - OVERFLOW_HEADROOM) {
            LOGw("%s: batch won't fit into context, shifting", __func__);
            shift_context();
        }

        for (int j = 0; j < cur_batch_size; j++) {
            const llama_token token_id = tokens[i + j];
            const llama_pos position = start_pos + i + j;
            const bool want_logit = compute_last_logit && (i + j == tokens.size() - 1);
            batch.add(token_id, position, 0, want_logit);
        }

        const int decode_result = llama_process(context, LLAMA_PROCESS_TYPE_DECODE, batch.get());
        if (decode_result) {
            LOGe("%s: llama_decode failed w/ %d", __func__, decode_result);
            return 1;
        }
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeSetChatTemplate(JNIEnv *env, jobject /*unused*/, jstring jtemplate) {
    if (jtemplate == nullptr) {
        g_chat_template_override.clear();
        return;
    }
    const auto *t = env->GetStringUTFChars(jtemplate, nullptr);
    g_chat_template_override.assign(t);
    env->ReleaseStringUTFChars(jtemplate, t);
    LOGi("%s: chat template override set (%zu bytes)", __func__, g_chat_template_override.size());
}

extern "C"
JNIEXPORT jint JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeProcessSystemPrompt(
        JNIEnv *env,
        jobject /*unused*/,
        jstring jsystem_prompt
) {
  try {
    reset_long_term_states();
    reset_short_term_states();

    const auto *system_prompt = env->GetStringUTFChars(jsystem_prompt, nullptr);
    std::string formatted_system_prompt(system_prompt);

    const bool has_chat_template = common_chat_templates_was_explicit(g_chat_templates.get());
    if (has_chat_template) {
        formatted_system_prompt = chat_add_and_format(ROLE_SYSTEM, system_prompt);
    }
    env->ReleaseStringUTFChars(jsystem_prompt, system_prompt);

    const auto system_tokens = common_tokenize(g_context, formatted_system_prompt,
                                               has_chat_template, has_chat_template);
    const int max_batch_size = DEFAULT_CONTEXT_SIZE - OVERFLOW_HEADROOM;
    if ((int) system_tokens.size() > max_batch_size) {
        LOGe("%s: system prompt too long: %d tokens, max %d",
             __func__, (int) system_tokens.size(), max_batch_size);
        return 1;
    }

    if (decode_tokens_in_batches(g_context, g_batch, system_tokens, current_position)) {
        LOGe("%s: llama_decode() failed", __func__);
        return 2;
    }

    system_prompt_position = current_position = (int) system_tokens.size();
    return 0;
  } catch (const std::exception &e) {
    LOGe("%s: %s", __func__, e.what());
    return 3;
  }
}

extern "C"
JNIEXPORT jint JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeProcessUserPrompt(
        JNIEnv *env,
        jobject /*unused*/,
        jstring juser_prompt,
        jint n_predict
) {
  try {
    reset_short_term_states();

    const auto *const user_prompt = env->GetStringUTFChars(juser_prompt, nullptr);
    std::string formatted_user_prompt(user_prompt);

    const bool has_chat_template = common_chat_templates_was_explicit(g_chat_templates.get());
    if (has_chat_template) {
        formatted_user_prompt = chat_add_and_format(ROLE_USER, user_prompt);
    }
    env->ReleaseStringUTFChars(juser_prompt, user_prompt);

    auto user_tokens = common_tokenize(g_context, formatted_user_prompt, has_chat_template, has_chat_template);

    const int user_prompt_size = (int) user_tokens.size();
    const int max_batch_size = DEFAULT_CONTEXT_SIZE - OVERFLOW_HEADROOM;
    if (user_prompt_size > max_batch_size) {
        const int skipped_tokens = user_prompt_size - max_batch_size;
        user_tokens.resize(max_batch_size);
        LOGw("%s: user prompt too long, skipped %d tokens", __func__, skipped_tokens);
    }

    if (decode_tokens_in_batches(g_context, g_batch, user_tokens, current_position, true)) {
        LOGe("%s: llama_decode() failed", __func__);
        return 2;
    }

    current_position += user_prompt_size;
    stop_generation_position = current_position + user_prompt_size + n_predict;
    return 0;
  } catch (const std::exception &e) {
    LOGe("%s: %s", __func__, e.what());
    return 3;
  }
}

// Process a user prompt that carries one image. The prompt text is wrapped
// in the chat template with the mtmd media marker, the formatted string is
// split on the marker, and text segments interleaved with the bitmap are
// tokenized and evaluated through mtmd (mirrors mtmd-cli.cpp's flow).
// Returns 0 on success; 1 no vision adapter, 2 image unreadable,
// 3 tokenize failed, 4 image does not fit the remaining context,
// 5 evaluation failed, 6 unexpected error.
extern "C"
JNIEXPORT jint JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeProcessUserPromptWithImage(
        JNIEnv *env,
        jobject /*unused*/,
        jstring juser_prompt,
        jstring jimage_path,
        jint n_predict
) {
  try {
    if (g_mtmd == nullptr) {
        return 1;
    }
    reset_short_term_states();

    const auto *user_prompt = env->GetStringUTFChars(juser_prompt, nullptr);
    const std::string marker = mtmd_default_marker();
    std::string content = marker + "\n" + user_prompt;
    env->ReleaseStringUTFChars(juser_prompt, user_prompt);

    const bool has_chat_template = common_chat_templates_was_explicit(g_chat_templates.get());
    const bool add_bos = chat_msgs.empty();
    std::string formatted;
    if (has_chat_template) {
        formatted = chat_add_and_format(ROLE_USER, content);
    } else {
        formatted = content;
    }

    const auto *image_path = env->GetStringUTFChars(jimage_path, nullptr);
    auto opt = mtmd_helper_init_opt_default();
    auto bw = mtmd_helper_bitmap_init_from_file(g_mtmd, image_path, false, opt);
    env->ReleaseStringUTFChars(jimage_path, image_path);
    if (!bw.bitmap) {
        LOGe("%s: could not decode image", __func__);
        return 2;
    }

    // Split the formatted prompt on the media marker; exactly one image is
    // supported here, so exactly two segments are expected.
    std::vector<std::string> segments;
    size_t start = 0;
    size_t pos;
    while ((pos = formatted.find(marker, start)) != std::string::npos) {
        segments.push_back(formatted.substr(start, pos - start));
        start = pos + marker.size();
    }
    segments.push_back(formatted.substr(start));
    if (segments.size() != 2) {
        LOGe("%s: media marker count mismatch (%zu segments)", __func__, segments.size());
        mtmd_bitmap_free(bw.bitmap);
        return 3;
    }

    std::vector<mtmd_input_text> texts(segments.size());
    std::vector<mtmd_input_part> parts;
    std::vector<const mtmd_input_part *> parts_ptr;
    for (size_t i = 0; i < segments.size(); i++) {
        texts[i] = { segments[i].data(), segments[i].size(), /* add_special */ false, /* parse_special */ true };
        parts.push_back({ &texts[i], nullptr });
        if (i == 0) {
            parts.push_back({ nullptr, bw.bitmap });
        }
    }
    for (auto &p : parts) {
        parts_ptr.push_back(&p);
    }

    mtmd_input_chunks *chunks = mtmd_input_chunks_init();
    int32_t res = mtmd_tokenize_from_parts(g_mtmd, chunks, parts_ptr.data(), parts_ptr.size(), add_bos);
    if (res != 0) {
        LOGe("%s: mtmd_tokenize_from_parts failed w/ %d", __func__, res);
        mtmd_input_chunks_free(chunks);
        mtmd_bitmap_free(bw.bitmap);
        return 3;
    }

    const llama_pos n_pos = mtmd_helper_get_n_pos(chunks);
    if (current_position + n_pos > DEFAULT_CONTEXT_SIZE - OVERFLOW_HEADROOM) {
        LOGw("%s: image+prompt needs %d positions, only %d left", __func__,
             (int) n_pos, (int) (DEFAULT_CONTEXT_SIZE - OVERFLOW_HEADROOM - current_position));
        mtmd_input_chunks_free(chunks);
        mtmd_bitmap_free(bw.bitmap);
        return 4;
    }

    llama_pos new_n_past = current_position;
    res = mtmd_helper_eval_chunks(g_mtmd, g_context, chunks, current_position, 0,
                                  BATCH_SIZE, /* logits_last */ true, &new_n_past);
    mtmd_input_chunks_free(chunks);
    mtmd_bitmap_free(bw.bitmap);
    if (res != 0) {
        LOGe("%s: mtmd_helper_eval_chunks failed w/ %d", __func__, res);
        return 5;
    }

    current_position = new_n_past;
    stop_generation_position = current_position + n_predict;
    return 0;
  } catch (const std::exception &e) {
    LOGe("%s: %s", __func__, e.what());
    return 6;
  }
}

static bool is_valid_utf8(const char *string) {
    if (!string) { return true; }

    const auto *bytes = (const unsigned char *) string;
    int num;

    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            num = 4;
        } else {
            return false;
        }

        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }
    return true;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeGenerateNextToken(
        JNIEnv *env,
        jobject /*unused*/
) {
    if (current_position >= DEFAULT_CONTEXT_SIZE - OVERFLOW_HEADROOM) {
        LOGw("%s: context full, shifting", __func__);
        shift_context();
    }

    if (current_position >= stop_generation_position) {
        return nullptr;
    }

    const auto new_token_id = common_sampler_sample(g_sampler, g_context, -1);
    common_sampler_accept(g_sampler, new_token_id, true);

    g_batch.clear();
    g_batch.add(new_token_id, current_position, 0, true);
    if (llama_process(g_context, LLAMA_PROCESS_TYPE_DECODE, g_batch.get()) != 0) {
        LOGe("%s: llama_decode() failed for generated token", __func__);
        return nullptr;
    }

    current_position++;

    if (llama_vocab_is_eog(llama_model_get_vocab(g_model), new_token_id)) {
        chat_add_and_format(ROLE_ASSISTANT, assistant_ss.str());
        return nullptr;
    }

    auto new_token_chars = common_token_to_piece(g_context, new_token_id);
    cached_token_chars += new_token_chars;

    jstring result = nullptr;
    if (is_valid_utf8(cached_token_chars.c_str())) {
        result = env->NewStringUTF(cached_token_chars.c_str());
        assistant_ss << cached_token_chars;
        cached_token_chars.clear();
    } else {
        result = env->NewStringUTF("");
    }
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeUnload(JNIEnv * /*unused*/, jobject /*unused*/) {
    reset_long_term_states();
    reset_short_term_states();

    common_sampler_free(g_sampler);
    g_chat_templates.reset();
    g_batch = common_batch();
    if (g_mtmd != nullptr) {
        mtmd_free(g_mtmd);
        g_mtmd = nullptr;
    }
    llama_free(g_context);
    llama_model_free(g_model);
    g_chat_template_override.clear();
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_aicon_studio_engine_llamacpp_LlamaCppEngine_nativeShutdown(JNIEnv *, jobject /*unused*/) {
    llama_backend_free();
}
