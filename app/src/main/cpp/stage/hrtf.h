// the measured ear responses (mit kemar compact set, bill gardner and keith martin) the room places sounds with
#pragma once

#include <cstddef>
#include <cstdint>
#include <memory>
#include <vector>

namespace stage {

struct HrtfSet {
    int taps = 0;
    int count = 0;
    // a unit vector per direction in head terms: x to the right, y up, z straight ahead
    std::vector<float> dirs;
    // taps per direction, stored back to front so a convolution reads both arrays forward
    std::vector<float> left, right;

    int nearest(float x, float y, float z) const;
    const float* leftOf(int dir) const { return left.data() + static_cast<size_t>(dir) * taps; }
    const float* rightOf(int dir) const { return right.data() + static_cast<size_t>(dir) * taps; }
};

// reads the asset once per process; false when it isn't the file this expects
bool loadHrtf(const uint8_t* data, size_t size);

// the set brought to a sample rate and shared by every engine at that rate, null until the asset is in
std::shared_ptr<const HrtfSet> hrtfFor(int sampleRate);

}
