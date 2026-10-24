#include <iostream>
#include <stdexcept>
#include "MelonDSNandJNI.cpp"

static void check(bool condition, const char* message) {
    if (!condition) throw std::runtime_error(message);
}
static void checkTitle(jobject title, unsigned expectedId) {
    check(title && title->kind == JavaObject::Title, "missing title");
    check(title->titleId == expectedId, "wrong title ID/order");
    check(title->children.size() == 3, "constructor data lost");
    check(title->children[0]->text == "Test title", "title text lost");
    check(title->children[1]->text == "Test producer", "producer text lost");
    check(title->children[2]->bytes.size() == 4096, "icon bytes lost");
    check(static_cast<unsigned char>(title->children[2]->bytes[0]) == 0x78, "icon content lost");
}
int main() {
    try {
        u8 key[16]{};
        melonDS::DSi_NAND::NANDImage image(nullptr, key);
        melonDS::DSi_NAND::NANDMount mount(image);
        nandMount = &mount;
        bool bounded = true;
        {
            JNIEnv env;
            auto title = getTitleData(&env, DSI_NAND_FILE_CATEGORY, 42);
            checkTitle(title, 42);
            std::cout << "single title locals=" << env.liveLocals << " peak=" << env.peakLocals << '\n';
            bounded &= env.liveLocals == 1;
            env.DeleteLocalRef(title);
            bounded &= env.liveLocals == 0;
        }
        for (unsigned count : {0u, 1u, 300u, 1500u}) {
            mount.titles.clear();
            for (unsigned i=0; i<count; ++i) mount.titles.push_back(i+100);
            JNIEnv env;
            auto list = Java_me_magnum_melonds_MelonDSiNand_listTitles(&env, nullptr);
            check(list && list->children.size() == count, "list incomplete");
            env.requireLocal(list);
            for (unsigned i=0; i<count; ++i) checkTitle(list->children[i], i+100);
            std::cout << "list count=" << count << " locals=" << env.liveLocals << " peak=" << env.peakLocals << '\n';
            bounded &= env.liveLocals == 1;
            bounded &= env.peakLocals <= 7;
            check(!env.ExceptionCheck(), "unexpected Java exception");
            env.DeleteLocalRef(list);
            bounded &= env.liveLocals == 0;
        }
        check(bounded, "temporary JNI local references leaked or grew with title count");
        std::cout << "PASS: title content/order survives temporary local cleanup; constant JNI ref usage\n";
    } catch (const std::exception& e) {
        std::cerr << "FAIL: " << e.what() << '\n';
        return 1;
    }
}
