/*
    Copyright 2016-2022 melonDS team

    This file is part of melonDS.

    melonDS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    melonDS is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with melonDS. If not, see http://www.gnu.org/licenses/.
*/

#ifndef SAVEMANAGER_H
#define SAVEMANAGER_H

#include <atomic>
#include <string>
#include <unistd.h>
#include <time.h>

#include "types.h"
#include "Platform.h"

using namespace melonDS;
using namespace melonDS::Platform;

class SaveManager
{
public:
    SaveManager(std::string path);
    ~SaveManager();

    std::string GetPath();
    void SetPath(std::string path, bool reload);

    void RequestFlush(const u8* savedata, u32 savelen, u32 writeoffset, u32 writelen);
    void CheckFlush();

    bool NeedsFlush();
    // Returns false whenever the requested flush did not happen: flushing to a file
    // leaves the pending generation eligible for a later retry, flushing to memory
    // leaves dst untouched. Returns true when there was nothing pending to write.
    bool FlushSecondaryBuffer(u8* dst = nullptr, u32 dstLength = 0);

    // Signalling for the periodic worker: a non-zero count means the last periodic
    // flush attempt failed and the staged generation is still unwritten.
    u32 GetConsecutiveFlushFailures();
    bool HasPendingFlushError();

private:

    void run();

    std::string Path;

    std::atomic_bool Running;

    std::unique_ptr<u8[]> Buffer;
    u32 Length;
    bool FlushRequested;

    Platform::Thread* Thread;
    Platform::Mutex* SecondaryBufferLock;
    std::unique_ptr<u8[]> SecondaryBuffer;
    u32 SecondaryBufferLength;

    time_t TimeAtLastFlushRequest;

    // We keep versions in case the user closes the application before
    // a flush cycle is finished.
    u32 PreviousFlushVersion;
    u32 FlushVersion;

    // Written by the periodic worker, polled by the owner; kept atomic because the
    // rest of this class stays as unsynchronised as it was.
    std::atomic<u32> ConsecutiveFlushFailures;
};

#endif // SAVEMANAGER_H
