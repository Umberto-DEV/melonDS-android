#ifndef MELONINSTANCE_H
#define MELONINSTANCE_H

#include <atomic>
#include <string>
#include "Args.h"
#include "Configuration.h"
#include "NDS.h"
#include "MelonDS.h"
#include "SaveManager.h"
#include "RewindManager.h"
#include "renderer/FrameQueue.h"
#include "renderer/Renderer.h"
#include "renderer/ScreenshotRenderer.h"
#include "retroachievements/RetroAchievementsManager.h"
#include "net/Net.h"

using namespace melonDS;

namespace MelonDSAndroid
{

/**
 * Copies one WFC slot into a firmware access point block and refreshes its checksum. The SSID is
 * only replaced when @c slotData.name is not empty, so a caller that doesn't manage names (see
 * WfcSettingsJNI.cpp) leaves the stored one alone.
 */
void applyWfcSlotData(melonDS::Firmware::WifiAccessPoint& accessPoint, const WfcSlotData& slotData);

/** Reads one firmware access point block into a WFC slot. */
void extractWfcSlotData(const melonDS::Firmware::WifiAccessPoint& accessPoint, WfcSlotData& slotData);

/**
 * Whether the three access point blocks (and, for DSi firmware, the three extended ones) really
 * are inside the firmware buffer. The offsets come from the firmware header's user settings
 * offset (Firmware::GetUserDataOffset()), which a truncated or bogus image can put anywhere.
 */
bool wfcAccessPointsInBounds(const melonDS::Firmware& firmware);

class MelonInstance
{

public:
    MelonInstance(int instanceId, std::shared_ptr<EmulatorConfiguration> configuration, std::unique_ptr<melonDS::NDSArgs> args, std::shared_ptr<Net> net, std::unique_ptr<ScreenshotRenderer> screenshotRenderer, int consoleType);
    ~MelonInstance();

    int getInstanceId() { return instanceId; };

    bool loadRom(std::string romPath, std::string sramPath);
    bool loadGbaRom(std::string romPath, std::string sramPath);
    void loadRumblePak();
    void loadGbaMemoryExpansion();
    void loadMotionPakHomebrew();
    void loadMotionPakRetail();
    bool bootFirmware();
    void start();
    void reset();
    melonDS::u32 runFrame();

    /**
     * Called when the emulator resumes after being paused (menu closed, app foregrounded,
     * ...). The DS RTC does not advance while the emulator is paused, so this is the point
     * at which the opt-in "Synchronize clock to device time" option gets a chance to catch
     * the DS clock up to the host -- see syncRTC() for the decision logic.
     */
    void onResumed();
    void stop();

    /**
     * Stages pending saves: copies the primary buffer into the secondary one for every
     * SaveManager, exactly as the frame loop does through CheckFlush().
     *
     * Call it ONLY from a thread ordered after the emulator thread - in practice after that
     * thread has been joined - because a flush request writes the primary buffer without a
     * lock from the emulator thread: calling this while the emulator runs would copy under
     * an in-flight write.
     */
    void stageSaves();

    void updateMotionData(float ax, float ay, float az, float rx, float ry, float rz);
    float getMotionData(MotionQueryType type);
    void touchScreen(u16 x, u16 y);
    void releaseScreen();
    void pressKey(u32 key);
    void releaseKey(u32 key);
    int readAudioOutput(s16* buffer, int length);
    void setAudioOutputSkew(double skew);
    bool takeScreenshot();
    void loadCheats(std::list<Cheat> cheats);
    int sendNetPacket(u8* data, int length);
    int receiveNetPacket(u8* data);

    Frame* getPresentationFrame(std::optional<std::chrono::time_point<std::chrono::steady_clock>> deadline);

    void updateConfiguration(std::shared_ptr<EmulatorConfiguration> newConfiguration);
    void requestNdsSaveWrite(const u8* saveData, u32 saveLength, u32 writeOffset, u32 writeLength);
    void requestGbaSaveWrite(const u8* saveData, u32 saveLength, u32 writeOffset, u32 writeLength);
    void requestFirmwareSaveWrite(const u8* saveData, u32 saveLength, u32 writeOffset, u32 writeLength);
    /**
     * Reads the three Wi-fi access point slots out of the firmware buffer this instance serves
     * over SPI. Returns false if the firmware's layout doesn't make sense (see
     * wfcAccessPointsInBounds()).
     *
     * Call it ONLY with the emulator thread stopped: the console can write the same bytes
     * through the firmware SPI at any time (SPI.cpp, command 0x0A).
     */
    bool readWfcSlots(WfcSlotData slots[3]);

    /**
     * Writes one Wi-fi access point slot (0-2) into the firmware buffer and requests a flush of
     * the Wi-fi region to the firmware's backing file, exactly as the console's own writes do
     * (SPI.cpp, FirmwareMem::Release()). The DS re-reads the slots from SPI whenever it opens a
     * connection, so the new values are picked up without a reset.
     *
     * Call it ONLY with the emulator thread stopped, for the same reason as readWfcSlots().
     */
    bool writeWfcSlot(int slot, const WfcSlotData& slotData);

    bool saveState(Savestate* state);
    bool loadState(Savestate* state);
    RewindWindow getRewindWindow();
    bool loadRewindState(RewindSaveState rewindSaveState);
    void setupAchievements(
        std::list<RetroAchievements::RAAchievement> achievements,
        std::list<RetroAchievements::RALeaderboard> leaderboards,
        std::optional<std::string> richPresenceScript
    );
    void unloadRetroAchievementsData();
    std::string getRichPresenceStatus();
    std::vector<RetroAchievements::RARuntimeAchievement> getRuntimeAchievements();

private:
    void updateRenderer();
    void setBatteryLevels();
    void setDateTime();
    void syncRTC();
    void saveRewindState(RewindSaveState* rewindSaveState);

private:
    int instanceId;
    int consoleType;
    NDS* nds;
    std::shared_ptr<Net> net;

    std::atomic<float> motionData[6] = { 0.0f, 0.0f, 9.80665f, 0.0f, 0.0f, 0.0f };

    std::unique_ptr<RetroAchievements::RetroAchievementsManager> retroAchievementsManager;
    std::unique_ptr<SaveManager> ndsSave;
    std::unique_ptr<SaveManager> gbaSave;
    std::unique_ptr<SaveManager> firmwareSave;
    u32 inputMask;

    std::shared_ptr<EmulatorConfiguration> currentConfiguration;
    FrameQueue frameQueue;
    std::unique_ptr<ScreenshotRenderer> screenshotRenderer;
    RewindManager rewindManager;
    Renderer currentRenderer;
    bool isRenderConfigurationDirty;
    int frame;
};

}

#endif
