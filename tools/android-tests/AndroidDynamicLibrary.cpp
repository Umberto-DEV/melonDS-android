// Android fastmem resolves ASharedMemory_create through these platform entry points.
// Keep the existing HostPlatform.cpp unchanged; it supplies the other test services.
#include <Platform.h>
#include <dlfcn.h>

namespace melonDS::Platform
{
DynamicLibrary* DynamicLibrary_Load(const char* name)
{
    return reinterpret_cast<DynamicLibrary*>(dlopen(name, RTLD_NOW | RTLD_LOCAL));
}

void DynamicLibrary_Unload(DynamicLibrary* library)
{
    dlclose(reinterpret_cast<void*>(library));
}

void* DynamicLibrary_LoadFunction(DynamicLibrary* library, const char* name)
{
    return dlsym(reinterpret_cast<void*>(library), name);
}
}
