#pragma once
#include <algorithm>
#include <cassert>
#include <cstdarg>
#include <cstdint>
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>
#define JNIEXPORT
#define JNICALL
using jint = int;
using jlong = long long;
using jbyte = signed char;
using jboolean = unsigned char;
struct JavaObject {
    enum Kind { Class, List, Title, String, Bytes } kind;
    std::string text;
    std::vector<jbyte> bytes;
    std::vector<JavaObject*> children;
    jlong titleId = 0;
};
using jobject = JavaObject*;
using jclass = jobject;
using jstring = jobject;
using jbyteArray = jobject;
using jmethodID = int;

// A small JNI object/ref model, not a pattern-matching test. Objects remain reachable through
// the returned ArrayList after their temporary local handles have been released.
struct JNIEnv {
    std::vector<std::unique_ptr<JavaObject>> objects;
    std::unordered_map<jobject, int> locals;
    int peakLocals = 0;
    int liveLocals = 0;

    jobject allocate(JavaObject::Kind kind) {
        auto object = std::make_unique<JavaObject>();
        object->kind = kind;
        jobject result = object.get();
        objects.push_back(std::move(object));
        ++locals[result];
        peakLocals = std::max(peakLocals, ++liveLocals);
        return result;
    }
    void requireLocal(jobject object) { assert(object && locals[object] > 0); }
    jclass FindClass(const char* name) {
        auto result = allocate(JavaObject::Class);
        if (result) result->text = name;
        return result;
    }
    jmethodID GetMethodID(jclass cls, const char* name, const char*) {
        requireLocal(cls);
        return std::string(name) == "add" ? 2 : 1;
    }
    jobject NewObject(jclass cls, jmethodID method, ...) {
        requireLocal(cls);
        const bool list = cls->text == "java/util/ArrayList";
        auto result = allocate(list ? JavaObject::List : JavaObject::Title);
        if (!result || list) return result;
        va_list args;
        va_start(args, method);
        jobject title = va_arg(args, jobject);
        jobject producer = va_arg(args, jobject);
        result->titleId = va_arg(args, jlong);
        jobject icon = va_arg(args, jobject);
        requireLocal(title); requireLocal(producer); requireLocal(icon);
        result->children = {title, producer, icon};
        va_end(args);
        return result;
    }
    void CallVoidMethod(jobject list, jmethodID, jint index, jobject value) {
        requireLocal(list); requireLocal(value);
        assert(list->kind == JavaObject::List && value->kind == JavaObject::Title);
        assert(index == static_cast<jint>(list->children.size()));
        list->children.push_back(value);
    }
    jbyteArray NewByteArray(jint size) {
        auto result = allocate(JavaObject::Bytes);
        if (result) result->bytes.resize(size);
        return result;
    }
    jbyte* GetByteArrayElements(jbyteArray array, jboolean*) {
        requireLocal(array); return array->bytes.data();
    }
    void ReleaseByteArrayElements(jbyteArray array, jbyte*, jint) { requireLocal(array); }
    jstring NewStringUTF(const char* value) {
        auto result = allocate(JavaObject::String);
        if (result) result->text = value;
        return result;
    }
    const char* GetStringUTFChars(jstring string, jboolean* copy) {
        requireLocal(string); if (copy) *copy = false; return string->text.c_str();
    }
    void ReleaseStringUTFChars(jstring string, const char*) { requireLocal(string); }
    void DeleteLocalRef(jobject object) {
        if (!object) return;
        requireLocal(object); --locals[object]; --liveLocals;
    }
    jboolean ExceptionCheck() { return false; }
};
