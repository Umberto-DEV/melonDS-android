#include <fstream>
#include <cstdlib>
#include <unistd.h>
#include <iostream>
#include <stdexcept>
#include "MelonDSNandJNI.cpp"
static void check(bool condition, const char* message) {
    if (!condition) throw std::runtime_error(message);
}
static void makeFile(const std::string& name, size_t count) {
    std::ofstream out(name, std::ios::binary); std::string data(count, '\0'); out.write(data.data(), data.size());
}
static int runScenario(const std::string& scenario, const std::filesystem::path& root) {
    Audit::failExecutableImport = false;
    std::filesystem::create_directories(root);
    Audit::biosPath = (root / "bios.bin").string();
    Audit::nandPath = (root / "nand.bin").string();
    Audit::titleRoot = root / "titles";
    makeFile(Audit::biosPath, 0x8308 + 16);
    JNIEnv env;
    int result = 1;
    try {
        if (scenario == "missing_nand") {
            const auto code = Java_me_magnum_melonds_MelonDSiNand_openNand(&env, nullptr, nullptr);
            std::cout << "missing NAND open returned " << code << "; expected 3 (NAND_OPEN_FAILED)\n";
            check(code == 3, "NAND open failure was reported as success");
            check(!nand, "missing NAND must not retain native image");
        } else {
            makeFile(Audit::nandPath, 8);
            check(Java_me_magnum_melonds_MelonDSiNand_openNand(&env, nullptr, nullptr) == 0, "setup open failed");
            const auto titlePath = (root / "test-title.nds").string();
            makeFile(titlePath, 0x1000);
            const u32 titleWords[] = {0x4b414141, 0x00030004};
            auto file = std::fopen(titlePath.c_str(), "r+b");
            std::fseek(file, 0x230, SEEK_SET); std::fwrite(titleWords, sizeof(titleWords), 1, file); std::fclose(file);
            auto path = env.NewStringUTF(titlePath.c_str());
            auto metadata = env.NewByteArray(sizeof(melonDS::DSi_TMD::TitleMetadata) - (scenario == "short_metadata" ? 1 : 0));
            melonDS::DSi_TMD::TitleMetadata tmd{};
            const u8 id[] = {0x00,0x03,0x00,0x04,0x4b,0x41,0x41,0x41};
            std::memcpy(tmd.TitleId, id, sizeof(id));
            if (scenario == "metadata_mismatch") tmd.TitleId[4] = 0x4c;
            std::memcpy(metadata->bytes.data(), &tmd, metadata->bytes.size());
            if (scenario == "cleanup") {
                Audit::failExecutableImport = true;
                // A complete unrelated title must survive cleanup.
                const auto unrelated = Audit::titlePath(0x00030004, 0x4b424242);
                std::filesystem::create_directories(unrelated);
                makeFile((unrelated / "complete.app").string(), 4);
                const auto code = Java_me_magnum_melonds_MelonDSiNand_importTitle(&env, nullptr, path, metadata);
                const bool leftPartial = std::filesystem::exists(Audit::titlePath(0x00030004, 0x4b414141));
                std::cout << "failed import returned " << code << "; partial title remains=" << leftPartial << '\n';
                check(code == 5, "injected executable write failure should report install failure");
                check(!leftPartial, "failed import left title files at 00030004/4b414141");
                check(std::filesystem::exists(unrelated / "complete.app"), "cleanup damaged unrelated title");
            } else if (scenario == "metadata_mismatch") {
                // Metadata names a different valid title than the ROM.
                const auto code = Java_me_magnum_melonds_MelonDSiNand_importTitle(&env, nullptr, path, metadata);
                const bool wroteOtherTitle = std::filesystem::exists(Audit::titlePath(0x00030004, 0x4c414141));
                std::cout << "mismatched metadata returned " << code << "; wrong title written=" << wroteOtherTitle << '\n';
                check(code != 0 && !wroteOtherTitle, "mismatched TMD installed into a different title directory");
            } else if (scenario == "short_metadata" || scenario == "short_header") {
                if (scenario == "short_header") std::filesystem::resize_file(titlePath, 0x234);
                const auto code = Java_me_magnum_melonds_MelonDSiNand_importTitle(&env, nullptr, path, metadata);
                check(code != 0, "truncated input reported import success");
                check(!std::filesystem::exists(Audit::titlePath(0x00030004, 0x4b414141)), "truncated input mutated title storage");
            } else if (scenario == "success") {
                check(Java_me_magnum_melonds_MelonDSiNand_importTitle(&env, nullptr, path, metadata) == 0, "valid import failed");
                const auto installed = Audit::titlePath(0x00030004, 0x4b414141) / "complete.app";
                check(std::filesystem::exists(installed), "valid import missing title");
                makeFile(installed.string(), 13);
                check(Java_me_magnum_melonds_MelonDSiNand_importTitle(&env, nullptr, path, metadata) == 4, "duplicate import did not report already present");
                check(std::filesystem::file_size(installed) == 13, "duplicate import destroyed installed title");
            } else {
                throw std::runtime_error("unknown test scenario");
            }
            env.DeleteLocalRef(path); env.DeleteLocalRef(metadata);
        }
        std::cout << "PASS: " << scenario << '\n'; result = 0;
    } catch (const std::exception& e) { std::cerr << "FAIL: " << e.what() << '\n'; }
    Java_me_magnum_melonds_MelonDSiNand_closeNand(&env, nullptr);
    return result;
}

int main(int argc, char** argv) {
    if (argc == 3) return runScenario(argv[1], argv[2]);
    if (argc != 1) return 2;
    int result = 0;
    for (const char* scenario : {"missing_nand", "cleanup", "metadata_mismatch", "short_metadata", "short_header", "success"}) {
        char directory[] = "./nand-fixture-XXXXXX";
        if (!mkdtemp(directory)) return 2;
        result |= runScenario(scenario, directory);
        std::filesystem::remove_all(directory);
    }
    return result;
}
