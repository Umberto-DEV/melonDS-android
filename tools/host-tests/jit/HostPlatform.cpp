/*
 * A minimal melonDS::Platform implementation, just enough to link the core static library
 * into a host test binary. Only the file, thread and logging entry points do real work; the
 * rest (multiplayer, networking, camera, microphone, AAC, add-on cartridges and every "the
 * frontend should persist this" callback) are no-ops, because no host test needs them and a
 * test that started depending on one would be testing the stub rather than the core.
 *
 * The list is not padding: `nm -u libcore.a` reports exactly the 57 Platform symbols defined
 * below. Missing one is a link error, so this file has to stay in step with Platform.h.
 */

#include <Platform.h>

#include <condition_variable>
#include <cstdarg>
#include <cstdio>
#include <mutex>
#include <string>
#include <thread>

#include "FileModeString.h"

namespace melonDS::Platform
{

struct Thread { std::thread Handle; };
struct Mutex { std::mutex Handle; };
struct Semaphore { std::mutex Lock; std::condition_variable Signal; int Count = 0; };
struct AACDecoder {};
struct DynamicLibrary {};

// Raised by a test that wants to see the core's own diagnostics; otherwise only warnings and
// errors are printed, so a passing run stays readable.
bool VerboseLog = false;

void SignalStop(StopReason, void*) {}

// -- files -------------------------------------------------------------------------------

static FILE* AsFile(FileHandle* file) { return reinterpret_cast<FILE*>(file); }

FileHandle* OpenFile(const std::string& path, FileMode mode)
{
    FILE* probe = fopen(path.c_str(), "rb");
    const bool exists = probe != nullptr;
    if (probe)
        fclose(probe);

    const std::string modeString = MelonDSAndroid::GetStdioModeString(mode, exists);
    if (modeString.empty())
        return nullptr;

    return reinterpret_cast<FileHandle*>(fopen(path.c_str(), modeString.c_str()));
}

FileHandle* OpenLocalFile(const std::string& path, FileMode mode) { return OpenFile(path, mode); }

bool LocalFileExists(const std::string& name)
{
    FILE* file = fopen(name.c_str(), "rb");
    if (!file)
        return false;
    fclose(file);
    return true;
}

bool CloseFile(FileHandle* file) { return fclose(AsFile(file)) == 0; }
bool IsEndOfFile(FileHandle* file) { return feof(AsFile(file)) != 0; }
bool FileReadLine(char* str, int count, FileHandle* file) { return fgets(str, count, AsFile(file)) != nullptr; }
u64 FilePosition(FileHandle* file) { return static_cast<u64>(ftell(AsFile(file))); }

bool FileSeek(FileHandle* file, s64 offset, FileSeekOrigin origin)
{
    int whence = SEEK_SET;
    if (origin == FileSeekOrigin::Current) whence = SEEK_CUR;
    else if (origin == FileSeekOrigin::End) whence = SEEK_END;
    return fseek(AsFile(file), static_cast<long>(offset), whence) == 0;
}

void FileRewind(FileHandle* file) { rewind(AsFile(file)); }
u64 FileRead(void* data, u64 size, u64 count, FileHandle* file) { return fread(data, size, count, AsFile(file)); }
bool FileFlush(FileHandle* file) { return fflush(AsFile(file)) == 0; }
u64 FileWrite(const void* data, u64 size, u64 count, FileHandle* file) { return fwrite(data, size, count, AsFile(file)); }

u64 FileWriteFormatted(FileHandle* file, const char* fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    const int written = vfprintf(AsFile(file), fmt, args);
    va_end(args);
    return written < 0 ? 0 : static_cast<u64>(written);
}

u64 FileLength(FileHandle* file)
{
    FILE* handle = AsFile(file);
    const long position = ftell(handle);
    fseek(handle, 0, SEEK_END);
    const long length = ftell(handle);
    fseek(handle, position, SEEK_SET);
    return static_cast<u64>(length);
}

// -- logging -----------------------------------------------------------------------------

void Log(LogLevel level, const char* fmt, ...)
{
    if (!VerboseLog && level < LogLevel::Warn)
        return;

    va_list args;
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    va_end(args);
}

// -- threads, mutexes, semaphores --------------------------------------------------------

Thread* Thread_Create(std::function<void()> func) { return new Thread { std::thread(std::move(func)) }; }
void Thread_Free(Thread* thread) { delete thread; }
void Thread_Wait(Thread* thread) { if (thread->Handle.joinable()) thread->Handle.join(); }

Semaphore* Semaphore_Create() { return new Semaphore(); }
void Semaphore_Free(Semaphore* sema) { delete sema; }
void Semaphore_Reset(Semaphore* sema) { std::lock_guard<std::mutex> lock(sema->Lock); sema->Count = 0; }

void Semaphore_Wait(Semaphore* sema)
{
    std::unique_lock<std::mutex> lock(sema->Lock);
    sema->Signal.wait(lock, [sema] { return sema->Count > 0; });
    sema->Count--;
}

bool Semaphore_TryWait(Semaphore* sema, int)
{
    std::lock_guard<std::mutex> lock(sema->Lock);
    if (sema->Count <= 0)
        return false;
    sema->Count--;
    return true;
}

void Semaphore_Post(Semaphore* sema, int count)
{
    std::lock_guard<std::mutex> lock(sema->Lock);
    sema->Count += count;
    sema->Signal.notify_all();
}

Mutex* Mutex_Create() { return new Mutex(); }
void Mutex_Free(Mutex* mutex) { delete mutex; }
void Mutex_Lock(Mutex* mutex) { mutex->Handle.lock(); }
void Mutex_Unlock(Mutex* mutex) { mutex->Handle.unlock(); }
bool Mutex_TryLock(Mutex* mutex) { return mutex->Handle.try_lock(); }

// -- everything a host test has no use for -----------------------------------------------

void WriteNDSSave(const u8*, u32, u32, u32, void*) {}
void WriteGBASave(const u8*, u32, u32, u32, void*) {}
void WriteFirmware(const Firmware&, u32, u32, void*) {}
void WriteDateTime(int, int, int, int, int, int, void*) {}

void MP_Begin(void*) {}
void MP_End(void*) {}
int MP_SendPacket(u8*, int, u64, void*) { return 0; }
int MP_RecvPacket(u8*, u64*, void*) { return 0; }
int MP_SendCmd(u8*, int, u64, void*) { return 0; }
int MP_SendReply(u8*, int, u64, u16, void*) { return 0; }
int MP_SendAck(u8*, int, u64, void*) { return 0; }
int MP_RecvHostPacket(u8*, u64*, void*) { return 0; }
u16 MP_RecvReplies(u8*, u64, u16, void*) { return 0; }

int Net_SendPacket(u8*, int, void*) { return 0; }
int Net_RecvPacket(u8*, void*) { return 0; }

void Camera_Start(int, void*) {}
void Camera_Stop(int, void*) {}
void Camera_CaptureFrame(int, u32*, int, int, bool, void*) {}

void Mic_Start(void*) {}
void Mic_Stop(void*) {}
int Mic_ReadInput(s16*, int, void*) { return 0; }

AACDecoder* AAC_Init() { return nullptr; }
void AAC_DeInit(AACDecoder*) {}
bool AAC_Configure(AACDecoder*, int, int) { return false; }
bool AAC_DecodeFrame(AACDecoder*, const void*, int, void*, int) { return false; }

bool Addon_KeyDown(KeyType, void*) { return false; }
void Addon_RumbleStart(u32, void*) {}
void Addon_RumbleStop(void*) {}
float Addon_MotionQuery(MotionQueryType, void*) { return 0.f; }

}
