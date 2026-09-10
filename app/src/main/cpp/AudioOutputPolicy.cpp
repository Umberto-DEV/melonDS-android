#include "AudioOutputPolicy.h"

namespace MelonDSAndroid
{

bool shouldAudioOutputStreamBeActive(bool soundEnabled, int volume)
{
    return soundEnabled && volume > 0;
}

}
