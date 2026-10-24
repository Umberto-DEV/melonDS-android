// Host-only tests: synthetic RAM and code words, no ROM assets or Android dependency.
#include "WildEncounterToggle.h"
#include <algorithm>
#include <cassert>
#include <cstdio>
#include <vector>
using MelonDSAndroid::WildEncounterToggle;

struct Memory {
    std::vector<uint8_t> bytes = std::vector<uint8_t>(0x400000);
    unsigned writes = 0;
    uint8_t read8(uint32_t a) const { assert(a >= 0x02000000 && a < 0x02400000); return bytes[a - 0x02000000]; }
    uint32_t read32(uint32_t a) const { return read8(a) | (uint32_t(read8(a+1))<<8) | (uint32_t(read8(a+2))<<16) | (uint32_t(read8(a+3))<<24); }
    void write8(uint32_t a, uint8_t v) { assert(a >= 0x02000000 && a < 0x02400000); bytes[a - 0x02000000] = v; ++writes; }
    void write16(uint32_t a, uint16_t v) { write8(a,v); write8(a+1,v>>8); }
    void put32(uint32_t a, uint32_t v) { for (int i=0;i<4;++i) write8(a+i,v>>(i*8)); }
};
static constexpr uint32_t Manager=0x0226F27C, Field=0x022A0244, Events=0x022A1390;
static constexpr uint32_t Location=0x0227D4A0, Process=0x022A038C, Table=Events+0x920;
static std::vector<uint32_t> code() { return {
    0x52246C94,0x28038800,0x6211186C,0,0xB211186C,0,0xD5000000,25,
    0xC0000000,0x27,0xD7000000,0x32A48,0xD2000000,0,
    0x52246C94,0x28038800,0x6211186C,0,0xB211186C,0,0xD5000000,10,
    0xC0000000,0x0B,0xD8000000,0x32A3C,0xD2000000,0}; }
static Memory fieldMemory() {
    Memory m;
    m.put32(0x0211186C,Manager); m.put32(Manager+4,0x0203DEBD); m.put32(Manager+8,0x0203DED5);
    m.put32(Manager+0x1C,Field); m.put32(0x0203E304,0x021D4158); m.put32(0x021D4158,Field);
    m.put32(Field,Process); m.put32(Field+0x14,Events); m.put32(Field+0x20,Location);
    m.put32(Field+0x6C,1); m.put32(Location,96); m.put32(0x02246C94,0x28038800);
    m.write8(Table,25);
    for (int i=0;i<12;++i) m.write8(Table+8+i,17+i%3);
    for (int i=0;i<40;++i) m.write16(Table+20+i*2,10+i);
    m.writes=0; return m;
}
static int failures=0;
static void check(bool condition, const char* message) { if(!condition) { std::fprintf(stderr,"FAIL: %s\n",message); ++failures; } }
int main() {
    auto selection=WildEncounterToggle::parse(code());
    check(selection && selection->species==25 && selection->level==10,"exact selector parsed");
    for (size_t i=0;i<28;++i) if(i!=7 && i!=21) {auto bad=code();bad[i]^=1;check(!WildEncounterToggle::parse(bad),"modified code rejected");}
    auto bad=code();bad[7]=494;check(!WildEncounterToggle::parse(bad),"invalid species rejected");
    bad=code();bad[21]=0;check(!WildEncounterToggle::parse(bad),"invalid level rejected");
    check(WildEncounterToggle::supports("IPKE",0x19D1EEBB) && WildEncounterToggle::supports("IPKE",0x1C1F741C),"both supported checksums");
    check(!WildEncounterToggle::supports("IPGE",0x19D1EEBB) && !WildEncounterToggle::supports("IPKE",0),"unrelated ROM rejected");
    if(!selection) return 1;
    WildEncounterToggle toggle;
    toggle.configure(selection,0xFFF);
    auto m=fieldMemory(); const auto original=m.bytes;
    toggle.beforeFrame(m,0xFFF); toggle.afterFrame(m);
    check(!toggle.active() && m.bytes==original && m.writes==0,"starts off and performs no writes");
    toggle.beforeFrame(m,0xCFF);
    check(toggle.active() && m.read8(Table+8)==10 && m.read32(Table+20)==0x00190019,"L+R turns on and injects");
    toggle.afterFrame(m);check(m.bytes==original,"frame restores original full RAM");
    for(int i=0;i<50;++i) {toggle.beforeFrame(m,0xCFF);toggle.afterFrame(m);}
    check(toggle.active() && m.bytes==original,"held combo never repeats and no frame leaks");
    toggle.beforeFrame(m,0xFFF);toggle.afterFrame(m);
    toggle.beforeFrame(m,0xCFF);toggle.afterFrame(m);
    check(!toggle.active() && m.bytes==original,"released and pressed again turns off immediately");
    toggle.reset(0xCFF); toggle.beforeFrame(m,0xCFF);toggle.afterFrame(m);
    check(!toggle.active(),"reset/load while held requires release");
    toggle.beforeFrame(m,0xFFF);toggle.afterFrame(m);toggle.beforeFrame(m,0xCFF);
    m.put32(0x02246C94,0xFF4EF7F3);m.put32(Field+0x6C,0);toggle.afterFrame(m);
    check(m.read8(Table+8)==17 && m.read32(Table+20)==0x000B000A,"battle overlay transition still restores live table");
    m=fieldMemory(); toggle.beforeFrame(m,0xFFF);
    m.write16(Table+20,444);m.write8(Table+8,88);toggle.afterFrame(m);
    check(m.read8(Table+8)==88 && m.read32(Table+20)==0x000B01BC,"partial guest changes survive; untouched slots restore");
    m=fieldMemory();toggle.beforeFrame(m,0xFFF);m.put32(Location,97);
    for(int i=0;i<92;++i) m.write8(Table+8+i,0x77);
    const auto newMap=m.bytes;toggle.afterFrame(m);check(m.bytes==newMap,"new map table is not overwritten with old snapshot");
    m=fieldMemory();toggle.beforeFrame(m,0xFFF);m.put32(Manager+0x1C,0);
    const auto freed=m.bytes;toggle.afterFrame(m);check(m.bytes==freed,"destroyed field allocation is not written");
    m=fieldMemory();m.put32(Field+0x10,0x022B0000);m.writes=0;
    toggle.beforeFrame(m,0xFFF);toggle.afterFrame(m);check(m.writes==0,"no injection during map task");
    toggle.configure(WildEncounterToggle::Selection{493,100},0xFFF);check(!toggle.active(),"changed species rearms off");
    toggle.configure(std::nullopt,0xFFF);toggle.beforeFrame(m,0xCFF);toggle.afterFrame(m);check(!toggle.active(),"removed cheat cannot toggle on");
    std::printf("WildEncounterToggle: %s (%d failures)\n",failures?"FAIL":"PASS",failures);
    return failures?1:0;
}
