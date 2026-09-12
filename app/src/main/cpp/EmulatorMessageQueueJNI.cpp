#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstring>
#include <limits.h>
#include <mutex>
#include <Platform.h>

// messagePipes[0] -> read
// messagePipes[1] -> write
static int messagePipes[2] = { -1, -1 };

// Events now come from two producers (the emulation thread and the SaveManager worker), so header
// and payload must reach the pipe as a single write. The biggest payload today is 56 bytes (the
// achievement progress struct in AndroidMelonEventMessenger.cpp), far below PIPE_BUF (4096), which
// is what makes a single write atomic.
static const size_t kMaxEventSize = 256;
static std::mutex messagePipeMutex;
static_assert(kMaxEventSize <= PIPE_BUF, "An event must fit in a single atomic pipe write");

extern "C"
{

JNIEXPORT jint JNICALL
Java_me_magnum_melonds_impl_emulator_EmulatorMessageQueue_initMessagePipe(JNIEnv* env, jobject thiz)
{
    if (messagePipes[0] != -1) {
        return messagePipes[0];
    }

    if (pipe(messagePipes) == -1) {
        melonDS::Platform::Log(melonDS::Platform::LogLevel::Error, "Failed to create message queue pipes");
        return -1;
    }

    // Make read end non-blocking
    int flags = fcntl(messagePipes[0], F_GETFL, 0);
    fcntl(messagePipes[0], F_SETFL, flags | O_NONBLOCK);

    return messagePipes[0];
}

JNIEXPORT void JNICALL
Java_me_magnum_melonds_impl_emulator_EmulatorMessageQueue_closeMessagePipe(JNIEnv* env, jobject thiz)
{
    // Same lock as fireEmulatorEvent: a producer must not be writing to the fd while it is closed.
    std::lock_guard<std::mutex> lock(messagePipeMutex);

    if (messagePipes[0] != -1) {
        close(messagePipes[0]);
        messagePipes[0] = -1;
    }
    if (messagePipes[1] != -1) {
        close(messagePipes[1]);
        messagePipes[1] = -1;
    }
}

}

namespace MelonDSAndroid {
    void fireEmulatorEvent(int type, int dataLength, void* data) {
        struct {
            int type;
            int dataLength;
        } event = { type, dataLength };

        if (dataLength < 0 || sizeof(event) + (size_t) dataLength > kMaxEventSize) {
            melonDS::Platform::Log(melonDS::Platform::LogLevel::Error, "Emulator event %d payload of %d bytes is too large, dropping it", type, dataLength);
            return;
        }

        char buffer[kMaxEventSize];
        memcpy(buffer, &event, sizeof(event));
        if (data != nullptr)
            memcpy(buffer + sizeof(event), data, dataLength);

        std::lock_guard<std::mutex> lock(messagePipeMutex);
        if (messagePipes[1] == -1) {
            return;
        }

        write(messagePipes[1], buffer, sizeof(event) + dataLength);
    }
}