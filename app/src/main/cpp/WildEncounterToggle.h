#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace MelonDSAndroid {

/** SGP-only adapter for the exact grass selector code. Guest tables are borrowed
 * during RunFrame and restored before saves, rewind captures, or UI access.
 * This state belongs to the emulator, never to guest RAM or a save file. */
class WildEncounterToggle {
public:
    struct Selection {
        uint16_t species;
        uint8_t level;
        bool operator==(const Selection& other) const { return species == other.species && level == other.level; }
        bool operator!=(const Selection& other) const { return !(*this == other); }
    };

    static bool supports(const std::string& gameCode, uint32_t headerChecksum) {
        return gameCode == "IPKE" && (headerChecksum == 0x19D1EEBB || headerChecksum == 0x1C1F741C);
    }

    static bool supportsHeader(const uint8_t* header, size_t length) {
        if (!header || length < 0x200) return false;
        // The cheat database uses the uncomplemented CRC32 register after 512 bytes.
        uint32_t crc = 0xFFFFFFFF;
        for (size_t i = 0; i < 0x200; ++i) {
            crc ^= header[i];
            for (int bit = 0; bit < 8; ++bit) crc = (crc >> 1) ^ ((crc & 1) ? 0xEDB88320 : 0);
        }
        return supports(std::string(reinterpret_cast<const char*>(header + 12), 4), crc);
    }

    static std::optional<Selection> parse(const std::vector<uint32_t>& code) {
        static constexpr std::array<uint32_t, 28> expected = {
            0x52246C94,0x28038800,0x6211186C,0,0xB211186C,0,0xD5000000,0,
            0xC0000000,0x27,0xD7000000,0x32A48,0xD2000000,0,
            0x52246C94,0x28038800,0x6211186C,0,0xB211186C,0,0xD5000000,0,
            0xC0000000,0x0B,0xD8000000,0x32A3C,0xD2000000,0,
        };
        if (code.size() != expected.size()) return std::nullopt;
        for (size_t i = 0; i < expected.size(); ++i)
            if (i != 7 && i != 21 && code[i] != expected[i]) return std::nullopt;
        if (code[7] < 1 || code[7] > 493 || code[21] < 1 || code[21] > 100) return std::nullopt;
        return Selection{static_cast<uint16_t>(code[7]), static_cast<uint8_t>(code[21])};
    }

    void configure(std::optional<Selection> next, uint32_t keys) {
        if (selection != next) {
            selection = next;
            reset(keys);
        }
    }

    // Called between frames on ROM/reset/state load. A held combo must be released.
    void reset(uint32_t keys) {
        enabled = false;
        comboWasDown = comboDown(keys);
        snapshot.reset();
    }

    bool active() const { return enabled; }

    template<class Memory> void beforeFrame(Memory& memory, uint32_t keys) {
        const bool down = comboDown(keys);
        if (selection && down && !comboWasDown) enabled = !enabled;
        comboWasDown = down;
        if (!selection || !enabled) return;
        const auto owner = fieldOwner(memory);
        if (!owner || memory.read32(0x02246C94) != 0x28038800 ||
            memory.read32(owner->field + 0x6C) != 1 || memory.read32(owner->field + 0x10) != 0) return;
        const auto process = memory.read32(owner->field);
        if (!ram(process, 16) || memory.read32(process + 8) != 0) return;
        Snapshot saved{*owner, {}, {}};
        for (size_t i = 0; i < saved.levels.size(); ++i) {
            saved.levels[i] = memory.read8(owner->table + 8 + i);
            memory.write8(owner->table + 8 + i, selection->level);
        }
        for (size_t i = 0; i < saved.species.size(); ++i) {
            const auto address = owner->table + 20 + i * 2;
            saved.species[i] = read16(memory, address);
            memory.write16(address, selection->species);
        }
        snapshot = saved;
    }

    template<class Memory> void afterFrame(Memory& memory) {
        if (!snapshot) return;
        const auto saved = *snapshot;
        snapshot.reset();
        const auto now = fieldOwner(memory);
        // The field overlay may already have become the battle overlay here.
        // Its instruction signature is deliberately NOT a restoration condition.
        if (!selection || !now || !now->sameTable(saved.owner)) return;
        // Restore units still owned by this injection. Preserve an entire species
        // halfword if the game changed it, rather than mixing old and new bytes.
        for (size_t i = 0; i < saved.levels.size(); ++i) {
            const auto address = now->table + 8 + i;
            if (memory.read8(address) == selection->level) memory.write8(address, saved.levels[i]);
        }
        for (size_t i = 0; i < saved.species.size(); ++i) {
            const auto address = now->table + 20 + i * 2;
            if (read16(memory, address) == selection->species) memory.write16(address, saved.species[i]);
        }
    }

private:
    struct Owner {
        uint32_t manager, field, events, location, map, table;
        bool sameTable(const Owner& other) const {
            return manager == other.manager && field == other.field && events == other.events &&
                location == other.location && map == other.map;
        }
    };
    struct Snapshot { Owner owner; std::array<uint8_t, 12> levels; std::array<uint16_t, 40> species; };
    std::optional<Selection> selection;
    std::optional<Snapshot> snapshot;
    bool enabled = false;
    bool comboWasDown = false;

    static bool comboDown(uint32_t keys) { return (keys & 0x300) == 0; }
    static bool ram(uint32_t address, uint32_t size) {
        return address >= 0x02000000 && address <= 0x02400000 - size && (address & 3) == 0;
    }
    template<class Memory> static uint16_t read16(Memory& memory, uint32_t address) {
        return uint16_t(memory.read8(address)) | (uint16_t(memory.read8(address + 1)) << 8);
    }
    template<class Memory> static std::optional<Owner> fieldOwner(Memory& memory) {
        const auto manager = memory.read32(0x0211186C);
        if (!ram(manager, 0x28)) return std::nullopt;
        // Field_AppExec / Field_AppExit are shared by Continue and New Game.
        if (memory.read32(manager + 4) != 0x0203DEBD || memory.read32(manager + 8) != 0x0203DED5) return std::nullopt;
        const auto field = memory.read32(manager + 0x1C);
        const auto fieldGlobal = memory.read32(0x0203E304);
        if (!ram(field, 0x70) || !ram(fieldGlobal, 4) || memory.read32(fieldGlobal) != field) return std::nullopt;
        const auto events = memory.read32(field + 0x14);
        const auto location = memory.read32(field + 0x20);
        if (!ram(events, 0x9E4) || !ram(location, 20)) return std::nullopt;
        const auto table = events + 0x920;
        if (table != manager + 0x32A34) return std::nullopt;
        return Owner{manager, field, events, location, memory.read32(location), table};
    }
};

} // namespace MelonDSAndroid
