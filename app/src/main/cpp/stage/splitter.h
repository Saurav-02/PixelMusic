// pulls a stereo song apart into what the room can place: the sub under a crossover, then voice, punch, left, right and the ambience pair out of an fft
#pragma once

#include "dsp.h"

namespace stage {

enum Band { BandVocal, BandCenter, BandLeft, BandRight, BandAmbL, BandAmbR, BandCount };

class Splitter {
public:
    explicit Splitter(int sampleRate);

    // how many samples late every band comes out
    int latency() const { return n_; }

    // one stereo frame in, the bands and sub of the frame latency() samples ago out
    void push(float l, float r, float* bands, float& sub);

    void clear();

private:
    void frame();

    int n_, hop_, pos_ = 0;
    Fft fft_;
    std::vector<float> window_, voice_;
    std::vector<float> inL_, inR_;
    std::vector<float> acc_[BandCount], ready_[BandCount];
    // per bin: smoothed powers and cross term, then the smoothed masks
    std::vector<float> pLL_, pRR_, pLRre_, pLRim_, gCenter_, gAmbL_, gAmbR_;
    std::vector<float> re_, im_, outRe_[3], outIm_[3];
    float alpha_, beta_;
    Biquad highL_[2], highR_[2], low_[2];
    std::vector<float> subDelay_;
    int subPos_ = 0;
};

}
