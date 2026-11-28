// the small pieces every part of the 3d room shares: an fft, a biquad, a one-pole and a ring of past samples
#pragma once

#include <algorithm>
#include <cmath>
#include <vector>

namespace stage {

constexpr float Pi = 3.14159265358979f;
constexpr float SpeedOfSound = 343.f;

// an in-place radix-2 fft for one power-of-two size, twiddles and bit order worked out once
class Fft {
public:
    explicit Fft(int n) : n_(n), order_(n), cos_(n / 2), sin_(n / 2) {
        int bits = 0;
        while ((1 << bits) < n) bits++;
        for (int i = 0; i < n; i++) {
            int r = 0;
            for (int b = 0; b < bits; b++) if (i & (1 << b)) r |= 1 << (bits - 1 - b);
            order_[i] = r;
        }
        for (int k = 0; k < n / 2; k++) {
            cos_[k] = std::cos(2 * Pi * k / n);
            sin_[k] = std::sin(2 * Pi * k / n);
        }
    }

    // the inverse is left unscaled, callers fold the 1/n into their own gain
    void run(float* re, float* im, bool inverse) const {
        for (int i = 0; i < n_; i++) {
            int j = order_[i];
            if (j > i) {
                std::swap(re[i], re[j]);
                std::swap(im[i], im[j]);
            }
        }
        for (int len = 2; len <= n_; len <<= 1) {
            int half = len / 2, step = n_ / len;
            for (int start = 0; start < n_; start += len) {
                for (int k = 0; k < half; k++) {
                    float wr = cos_[k * step], wi = inverse ? sin_[k * step] : -sin_[k * step];
                    int a = start + k, b = a + half;
                    float tr = re[b] * wr - im[b] * wi;
                    float ti = re[b] * wi + im[b] * wr;
                    re[b] = re[a] - tr;
                    im[b] = im[a] - ti;
                    re[a] += tr;
                    im[a] += ti;
                }
            }
        }
    }

private:
    int n_;
    std::vector<int> order_;
    std::vector<float> cos_, sin_;
};

// transposed direct form ii, enough for the crossover's butterworth halves
struct Biquad {
    float b0 = 1, b1 = 0, b2 = 0, a1 = 0, a2 = 0, z1 = 0, z2 = 0;

    void lowpass(float fs, float fc) { design(fs, fc, true); }
    void highpass(float fs, float fc) { design(fs, fc, false); }

    float run(float x) {
        float y = b0 * x + z1;
        z1 = b1 * x - a1 * y + z2;
        z2 = b2 * x - a2 * y;
        return y;
    }

    void clear() { z1 = z2 = 0; }

private:
    void design(float fs, float fc, bool low) {
        float w = 2 * Pi * fc / fs, c = std::cos(w), alpha = std::sin(w) / (2 * 0.70710678f), a0 = 1 + alpha;
        float edge = low ? (1 - c) / 2 : (1 + c) / 2;
        b0 = edge / a0;
        b1 = (low ? 1 - c : -(1 + c)) / a0;
        b2 = edge / a0;
        a1 = -2 * c / a0;
        a2 = (1 - alpha) / a0;
    }
};

// how far a one-pole lowpass moves toward its input each sample for a cutoff
inline float onePole(float fs, float fc) { return 1.f - std::exp(-2.f * Pi * std::min(fc, fs * 0.45f) / fs); }

// a power-of-two ring of past samples that can be read back at any fractional delay
struct Ring {
    std::vector<float> data;
    int mask = 0;
    int write = 0;

    void init(int minSize) {
        int n = 1;
        while (n < minSize) n <<= 1;
        data.assign(n, 0.f);
        mask = n - 1;
        write = 0;
    }

    int size() const { return mask + 1; }

    void push(float x) {
        data[write] = x;
        write = (write + 1) & mask;
    }

    // delay 0 is the sample pushed last
    float read(float delay) const {
        // kept positive so a plain cast floors it, std::floor was a third of the echoes' cost
        float pos = static_cast<float>(write - 1 + size()) - delay;
        int i = static_cast<int>(pos);
        float f = pos - static_cast<float>(i);
        float a = data[i & mask], b = data[(i + 1) & mask];
        return a + (b - a) * f;
    }

    void clear() { std::fill(data.begin(), data.end(), 0.f); }
};

}
