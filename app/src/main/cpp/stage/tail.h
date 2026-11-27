// the room's late tail: eight damped delay lines mixed through a hadamard matrix, sized and timed from the room's walls
#pragma once

#include "dsp.h"

namespace stage {

class Tail {
public:
    explicit Tail(float sampleRate);

    // the room in metres and its walls; glided toward over the next blocks, so dragging the size only bends the tail
    void setRoom(float width, float depth, float height, float absorption, float damping);

    // glides toward the room once per block
    void step(int frames);

    // one sample in, the tail's two sides out
    void run(float in, float& left, float& right);

    void clear();

private:
    static constexpr int Lines = 8;
    float fs_;
    Ring lines_[Lines];
    Ring pre_;
    float length_[Lines] = {}, lengthTarget_[Lines] = {};
    float feedback_[Lines] = {}, feedbackTarget_[Lines] = {};
    float damp_[Lines] = {};
    float dampCoef_ = 0.5f, dampTarget_ = 0.5f;
    float preDelay_ = 0.f, preTarget_ = 0.f;
    float norm_ = 0.f, normTarget_ = 0.f;
    bool primed_ = false;
};

}
