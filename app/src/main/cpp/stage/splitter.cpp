#include "splitter.h"

#include <cstring>

namespace stage {

namespace {
constexpr float Crossover = 90.f;
// how far from dead centre (on a -1..1 pan) something still counts as the middle of the mix
constexpr float CenterWidth = 0.3f;
constexpr float Tiny = 1e-12f;

int frameSize(int fs) { return fs <= 50000 ? 1024 : fs <= 100000 ? 2048 : 4096; }

// voice lives roughly between 150 hz and 8 khz, with soft edges so nothing rings
float voiceWeight(float f) {
    auto ramp = [](float x) { return 0.5f - 0.5f * std::cos(Pi * std::min(1.f, std::max(0.f, x))); };
    return ramp((f - 120.f) / 100.f) * (1.f - ramp((f - 7000.f) / 3000.f));
}
}

Splitter::Splitter(int sampleRate)
    : n_(frameSize(sampleRate)), hop_(n_ / 4), fft_(n_), window_(n_), voice_(n_ / 2 + 1),
      inL_(n_), inR_(n_), pLL_(n_ / 2 + 1), pRR_(n_ / 2 + 1), pLRre_(n_ / 2 + 1), pLRim_(n_ / 2 + 1),
      gCenter_(n_ / 2 + 1), gAmbL_(n_ / 2 + 1), gAmbR_(n_ / 2 + 1), re_(n_), im_(n_), subDelay_(n_) {
    float fs = static_cast<float>(sampleRate);
    // sqrt hann on both sides, so analysis times synthesis is a hann that sums flat at a quarter hop
    for (int i = 0; i < n_; i++) window_[i] = std::sqrt(0.5f - 0.5f * std::cos(2 * Pi * i / n_));
    for (int k = 0; k <= n_ / 2; k++) voice_[k] = voiceWeight(k * fs / n_);
    for (int b = 0; b < BandCount; b++) {
        acc_[b].assign(n_, 0.f);
        ready_[b].assign(hop_, 0.f);
    }
    for (int p = 0; p < 3; p++) {
        outRe_[p].assign(n_, 0.f);
        outIm_[p].assign(n_, 0.f);
    }
    float hopSeconds = hop_ / fs;
    alpha_ = std::exp(-hopSeconds / 0.04f);
    beta_ = std::exp(-hopSeconds / 0.02f);
    for (int i = 0; i < 2; i++) {
        highL_[i].highpass(fs, Crossover);
        highR_[i].highpass(fs, Crossover);
        low_[i].lowpass(fs, Crossover);
    }
}

void Splitter::push(float l, float r, float* bands, float& sub) {
    float hl = highL_[1].run(highL_[0].run(l));
    float hr = highR_[1].run(highR_[0].run(r));
    float lo = low_[1].run(low_[0].run((l + r) * 0.5f));
    inL_[n_ - hop_ + pos_] = hl;
    inR_[n_ - hop_ + pos_] = hr;
    for (int b = 0; b < BandCount; b++) bands[b] = ready_[b][pos_];
    // the sub waits exactly as long as the fft path, so the two line up again
    sub = subDelay_[subPos_];
    subDelay_[subPos_] = lo;
    subPos_ = subPos_ + 1 == n_ ? 0 : subPos_ + 1;
    if (++pos_ == hop_) {
        frame();
        pos_ = 0;
    }
}

void Splitter::frame() {
    int n = n_, half = n_ / 2;
    // both channels in one complex fft: left in the real part, right in the imaginary
    for (int i = 0; i < n; i++) {
        re_[i] = inL_[i] * window_[i];
        im_[i] = inR_[i] * window_[i];
    }
    fft_.run(re_.data(), im_.data(), false);

    for (int k = 0; k <= half; k++) {
        int j = (n - k) & (n - 1);
        float lr = (re_[k] + re_[j]) * 0.5f, li = (im_[k] - im_[j]) * 0.5f;
        float rr = (im_[k] + im_[j]) * 0.5f, ri = (re_[j] - re_[k]) * 0.5f;

        float a = alpha_;
        pLL_[k] = a * pLL_[k] + (1 - a) * (lr * lr + li * li);
        pRR_[k] = a * pRR_[k] + (1 - a) * (rr * rr + ri * ri);
        pLRre_[k] = a * pLRre_[k] + (1 - a) * (lr * rr + li * ri);
        pLRim_[k] = a * pLRim_[k] + (1 - a) * (li * rr - lr * ri);

        // the smaller eigenvalue of the channels' covariance is what the two sides don't share: the ambience
        float sum = pLL_[k] + pRR_[k], diff = pLL_[k] - pRR_[k];
        float cross = pLRre_[k] * pLRre_[k] + pLRim_[k] * pLRim_[k];
        float ambient = std::max(0.f, (sum - std::sqrt(diff * diff + 4 * cross)) * 0.5f);
        float ambL = std::min(1.f, ambient / (pLL_[k] + Tiny));
        float ambR = std::min(1.f, ambient / (pRR_[k] + Tiny));
        // where the shared part sits: 0 hard left, pi/4 the middle, pi/2 hard right, below 0 out of phase
        float theta = 0.5f * std::atan2(2 * pLRre_[k], diff);
        float pan = std::fabs((theta - Pi / 4) / (Pi / 4));
        float center = pan < CenterWidth ? 0.5f + 0.5f * std::cos(Pi * pan / CenterWidth) : 0.f;

        float b = beta_;
        gCenter_[k] = b * gCenter_[k] + (1 - b) * center;
        gAmbL_[k] = b * gAmbL_[k] + (1 - b) * ambL;
        gAmbR_[k] = b * gAmbR_[k] + (1 - b) * ambR;

        // the masks add back up to the song: primary plus ambience is each channel, centre plus sides is the primary
        float plR = (1 - gAmbL_[k]) * lr, plI = (1 - gAmbL_[k]) * li;
        float prR = (1 - gAmbR_[k]) * rr, prI = (1 - gAmbR_[k]) * ri;
        float cR = gCenter_[k] * (plR + prR) * 0.5f, cI = gCenter_[k] * (plI + prI) * 0.5f;
        float spectra[BandCount][2] = {
            {cR * voice_[k], cI * voice_[k]},
            {cR * (1 - voice_[k]), cI * (1 - voice_[k])},
            {plR - cR, plI - cI},
            {prR - cR, prI - cI},
            {gAmbL_[k] * lr, gAmbL_[k] * li},
            {gAmbR_[k] * rr, gAmbR_[k] * ri},
        };
        // two real bands per inverse fft: first in the real part, second in the imaginary
        for (int p = 0; p < 3; p++) {
            float ar = spectra[p * 2][0], ai = spectra[p * 2][1];
            float br = spectra[p * 2 + 1][0], bi = spectra[p * 2 + 1][1];
            if (k == 0 || k == half) {
                outRe_[p][k] = ar;
                outIm_[p][k] = br;
            } else {
                outRe_[p][k] = ar - bi;
                outIm_[p][k] = ai + br;
                outRe_[p][n - k] = ar + bi;
                outIm_[p][n - k] = br - ai;
            }
        }
    }

    // 1/n for the inverse fft, and a half because the hann windows overlap to two
    float gain = 0.5f / n;
    for (int p = 0; p < 3; p++) {
        fft_.run(outRe_[p].data(), outIm_[p].data(), true);
        float* first = acc_[p * 2].data();
        float* second = acc_[p * 2 + 1].data();
        for (int i = 0; i < n; i++) {
            float w = window_[i] * gain;
            first[i] += outRe_[p][i] * w;
            second[i] += outIm_[p][i] * w;
        }
    }
    for (int b = 0; b < BandCount; b++) {
        std::memcpy(ready_[b].data(), acc_[b].data(), hop_ * sizeof(float));
        std::memmove(acc_[b].data(), acc_[b].data() + hop_, (n - hop_) * sizeof(float));
        std::fill(acc_[b].begin() + (n - hop_), acc_[b].end(), 0.f);
    }
    std::memmove(inL_.data(), inL_.data() + hop_, (n - hop_) * sizeof(float));
    std::memmove(inR_.data(), inR_.data() + hop_, (n - hop_) * sizeof(float));
}

void Splitter::clear() {
    pos_ = 0;
    subPos_ = 0;
    std::fill(inL_.begin(), inL_.end(), 0.f);
    std::fill(inR_.begin(), inR_.end(), 0.f);
    for (int b = 0; b < BandCount; b++) {
        std::fill(acc_[b].begin(), acc_[b].end(), 0.f);
        std::fill(ready_[b].begin(), ready_[b].end(), 0.f);
    }
    for (auto* v : {&pLL_, &pRR_, &pLRre_, &pLRim_, &gCenter_, &gAmbL_, &gAmbR_}) std::fill(v->begin(), v->end(), 0.f);
    std::fill(subDelay_.begin(), subDelay_.end(), 0.f);
    for (int i = 0; i < 2; i++) {
        highL_[i].clear();
        highR_[i].clear();
        low_[i].clear();
    }
}

}
