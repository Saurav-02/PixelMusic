#include "hrtf.h"

#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstring>
#include <map>
#include <mutex>

namespace stage {

namespace {
constexpr float Pi = 3.14159265358979f;
constexpr int SincHalf = 16;
constexpr int MaxTaps = 1024;

// the asset as measured, both sides, taps front to back
struct Measured {
    int rate = 0, taps = 0;
    std::vector<float> dirs, left, right;
};

std::mutex lock;
std::shared_ptr<Measured> measured;
std::map<int, std::shared_ptr<const HrtfSet>> byRate;
std::atomic<bool> loaded{false};

template <typename T>
T readLe(const uint8_t* p) {
    T value;
    std::memcpy(&value, p, sizeof(T));
    return value;
}

void addDirection(Measured& m, float elevation, float azimuth, const float* l, const float* r) {
    float el = elevation * Pi / 180.f, az = azimuth * Pi / 180.f;
    m.dirs.push_back(std::cos(el) * std::sin(az));
    m.dirs.push_back(std::sin(el));
    m.dirs.push_back(std::cos(el) * std::cos(az));
    m.left.insert(m.left.end(), l, l + m.taps);
    m.right.insert(m.right.end(), r, r + m.taps);
}

float sinc(float x) { return std::fabs(x) < 1e-6f ? 1.f : std::sin(Pi * x) / (Pi * x); }

// windowed sinc from the measured rate to another, scaled so each response keeps its loudness
void resample(const float* in, int inTaps, float ratio, float* out, int outTaps) {
    float cut = std::min(1.f, 1.f / ratio);
    for (int t = 0; t < outTaps; t++) {
        float p = t / ratio;
        int from = std::max(0, static_cast<int>(std::floor(p)) - SincHalf);
        int to = std::min(inTaps - 1, static_cast<int>(std::floor(p)) + SincHalf);
        float sum = 0.f;
        for (int k = from; k <= to; k++) {
            float d = p - k;
            float w = 0.5f + 0.5f * std::cos(Pi * d / (SincHalf + 1));
            sum += in[k] * cut * sinc(cut * d) * w;
        }
        out[t] = sum / ratio;
    }
}

std::shared_ptr<const HrtfSet> build(const Measured& m, int sampleRate) {
    auto set = std::make_shared<HrtfSet>();
    float ratio = static_cast<float>(sampleRate) / m.rate;
    int count = static_cast<int>(m.dirs.size() / 3);
    int taps = std::min(MaxTaps, static_cast<int>(std::ceil(m.taps * ratio)));
    set->taps = taps;
    set->count = count;
    set->dirs = m.dirs;
    set->left.resize(static_cast<size_t>(count) * taps);
    set->right.resize(static_cast<size_t>(count) * taps);
    std::vector<float> scratch(taps);
    for (int d = 0; d < count; d++) {
        for (int ear = 0; ear < 2; ear++) {
            const float* src = (ear == 0 ? m.left : m.right).data() + static_cast<size_t>(d) * m.taps;
            if (sampleRate == m.rate) std::copy(src, src + taps, scratch.begin());
            else resample(src, m.taps, ratio, scratch.data(), taps);
            float* dst = (ear == 0 ? set->left : set->right).data() + static_cast<size_t>(d) * taps;
            for (int t = 0; t < taps; t++) dst[t] = scratch[taps - 1 - t];
        }
    }
    return set;
}
}

int HrtfSet::nearest(float x, float y, float z) const {
    int best = 0;
    float bestDot = -2.f;
    const float* d = dirs.data();
    for (int i = 0; i < count; i++, d += 3) {
        float dot = d[0] * x + d[1] * y + d[2] * z;
        if (dot > bestDot) {
            bestDot = dot;
            best = i;
        }
    }
    return best;
}

bool loadHrtf(const uint8_t* data, size_t size) {
    if (loaded.load()) return true;
    if (size < 24 || std::memcmp(data, "MHRT", 4) != 0) return false;
    int version = readLe<int32_t>(data + 4), rate = readLe<int32_t>(data + 8);
    int taps = readLe<int32_t>(data + 12), count = readLe<int32_t>(data + 16);
    float scale = readLe<float>(data + 20);
    size_t entry = 4 + static_cast<size_t>(taps) * 4;
    if (version != 1 || taps <= 0 || count <= 0 || size < 24 + entry * count) return false;

    auto m = std::make_shared<Measured>();
    m->rate = rate;
    m->taps = taps;
    std::vector<float> l(taps), r(taps);
    const uint8_t* p = data + 24;
    for (int i = 0; i < count; i++, p += entry) {
        float elevation = readLe<int16_t>(p), azimuth = readLe<int16_t>(p + 2);
        for (int t = 0; t < taps; t++) {
            l[t] = readLe<int16_t>(p + 4 + t * 2) * scale;
            r[t] = readLe<int16_t>(p + 4 + (taps + t) * 2) * scale;
        }
        addDirection(*m, elevation, azimuth, l.data(), r.data());
        // the set only covers the right side; the dummy head is symmetric, so the left is the same with the ears swapped
        if (azimuth > 0 && azimuth < 180) addDirection(*m, elevation, -azimuth, r.data(), l.data());
    }
    std::lock_guard<std::mutex> guard(lock);
    measured = m;
    byRate.clear();
    loaded.store(true);
    return true;
}

std::shared_ptr<const HrtfSet> hrtfFor(int sampleRate) {
    if (!loaded.load()) return nullptr;
    std::lock_guard<std::mutex> guard(lock);
    auto found = byRate.find(sampleRate);
    if (found != byRate.end()) return found->second;
    auto set = build(*measured, sampleRate);
    byRate[sampleRate] = set;
    return set;
}

}
