#ifndef MELONDS_MELONDS_H
#define MELONDS_MELONDS_H

#include <list>
#include <vector>
#include "AndroidFileHandler.h"
#include "AndroidCameraHandler.h"
#include "Configuration.h"
#include "MelonEventMessenger.h"
#include "RewindManager.h"
#include "RomGbaSlotConfig.h"
#include "retroachievements/RAAchievement.h"
#include "retroachievements/RALeaderboard.h"
#include "renderer/FrameQueue.h"
#include "types.h"
#include "../GPU.h"
#include <android/asset_manager.h>

using namespace melonDS;

namespace MelonDSAndroid {
    typedef struct {
        std::vector<u32> code;
    } Cheat;

    typedef enum {
        ROM,
        FIRMWARE
    } RunMode;

    /**
     * One of the three Wi-fi access point slots of the firmware, as the WFC screens see it.
     * @c name is the slot's SSID (NUL-terminated, at most 32 bytes, as in the firmware).
     */
    struct WfcSlotData {
        bool enabled;
        char name[33];
        u8 primaryDns[4];
        u8 secondaryDns[4];
    };

    /**
     * Reads the three Wi-fi access point slots of the firmware the running instance booted with
     * (generated or the user's file, DS or DSi). Returns false when no instance is running.
     * @warning The emulator thread must be stopped: see pauseEmulationThreadForSyncOperation().
     */
    extern bool readRunningInstanceWfcSlots(WfcSlotData slots[3]);

    /**
     * Writes one Wi-fi access point slot into the firmware buffer the running instance serves
     * over SPI, and requests a flush of the region to the firmware's backing file. The console
     * re-reads the slots from SPI on every connection, so the change applies without a reset.
     * Returns false when no instance is running or the firmware layout doesn't make sense.
     * @warning The emulator thread must be stopped: see pauseEmulationThreadForSyncOperation().
     */
    extern bool writeRunningInstanceWfcSlot(int slot, const WfcSlotData& slotData);

    /**
     * Stops the emulator thread for an operation that touches the whole machine state and waits
     * until it really is stopped. Returns whether it was already paused, which is what
     * resumeEmulationThreadAfterSyncOperation() needs to restore the previous state.
     * Implemented in MelonDSAndroidJNI.cpp, next to the thread it synchronises against.
     */
    extern bool pauseEmulationThreadForSyncOperation();
    extern void resumeEmulationThreadAfterSyncOperation(bool wasPaused);

    extern OpenGLContext *openGlContext;
    extern AndroidFileHandler* fileHandler;
    extern AndroidCameraHandler* cameraHandler;
    extern std::string internalFilesDir;
    extern std::shared_ptr<MelonEventMessenger> eventMessenger;

    extern void setConfiguration(EmulatorConfiguration emulatorConfiguration);
    extern void setup(AndroidCameraHandler* androidCameraHandler, std::shared_ptr<MelonEventMessenger> androidEventMessenger, u32* screenshotBufferPointer, int instanceId);
    extern void setCodeList(std::list<Cheat> cheats);
    extern void setupAchievements(std::list<RetroAchievements::RAAchievement> achievements, std::list<RetroAchievements::RALeaderboard> leaderboards, std::optional<std::string> richPresenceScript);
    extern void unloadRetroAchievementsData();
    extern std::string getRichPresenceStatus();
    extern std::vector<RetroAchievements::RARuntimeAchievement> getRuntimeAchievements();
    extern void updateEmulatorConfiguration(std::unique_ptr<EmulatorConfiguration> emulatorConfiguration);

    /**
     * Loads the NDS ROM and, optionally, the GBA ROM.
     *
     * @param romPath The path to the NDS rom
     * @param sramPath The path to the rom's SRAM file
     * @param gbaSlotConfig The config to be used for the GBA slot
     * @return The load result. 0 if everything was loaded successfully, 1 if the NDS ROM was loaded but the GBA ROM
     * failed to load, 2 if the NDS ROM failed to load, 3 if there is no emulator instance because setup() failed
     */
    extern int loadRom(std::string romPath, std::string sramPath, RomGbaSlotConfig* gbaSlotConfig);
    extern int bootFirmware();
    extern void touchScreen(u16 x, u16 y);
    extern void releaseScreen();
    extern void pressKey(u32 key);
    extern void releaseKey(u32 key);
    extern void updateMotionData(float ax, float ay, float az, float rx, float ry, float rz);
    extern void start();
    extern u32 loop();
    extern Frame* getPresentationFrame(std::optional<std::chrono::time_point<std::chrono::steady_clock>> deadline);
    extern void pause();
    extern void resume();
    extern void reset();
    extern bool saveState(const char* path);
    extern bool loadState(const char* path);
    extern bool loadRewindState(melonDS::RewindSaveState rewindSaveState);
    extern RewindWindow getRewindWindow();
    extern bool takeScreenshot();
    /** Stages pending saves for the final flush. Call only after the emulator thread has been joined. */
    extern void stageSaves();
    extern void stop();
    extern void cleanup();
}

#endif //MELONDS_MELONDS_H
