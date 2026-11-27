#include "tail.h"

namespace stage {

namespace {
// spread so no two lines share a pattern and the tail never rings on one note
constexpr float Spread[8] = {1.000f, 1.137f, 1.271f, 1.414f, 1.553f, 1.693f, 1.836f, 1.972f};
constexpr float MaxSeconds = 0.6f;
}

Tail::Tail(float sampleRate) : fs_(sampleRate) {
    for (auto& line : lines_) line.init(static_cast<int>(MaxSeconds * sampleRate) + 8);
    pre_.init(static_cast<int>(0.15f * sampleRate) + 8);
}

void Tail::setRoom(float width, float depth, float height, float absorption, float damping) {
    float volume = width * depth * height;
    float surface = 2 * (width * depth + width * height + depth * height);
    float freePath = 4 * volume / surface;
    // sabine, held to what still sounds like a room and not a cave
    float rt60 = std::min(3.5f, std::max(0.15f, 0.161f * volume / (surface * std::max(0.02f, absorption))));
    float base = 0.025f + 3 * freePath / SpeedOfSound;
    float meanSquare = 0;
    for (int i = 0; i < Lines; i++) {
        float seconds = std::min(MaxSeconds - 0.01f, base * Spread[i]);
        lengthTarget_[i] = seconds * fs_;
        feedbackTarget_[i] = std::pow(10.f, -3.f * seconds / rt60);
        meanSquare += feedbackTarget_[i] * feedbackTarget_[i] / Lines;
    }
    // a loop that keeps more of itself piles up more energy, so the level is evened out by how long it rings
    normTarget_ = std::sqrt(1 - meanSquare);
    dampTarget_ = onePole(fs_, 12000.f - 9500.f * damping);
    preTarget_ = std::min(0.14f, freePath / SpeedOfSound) * fs_;
    if (!primed_) {
        for (int i = 0; i < Lines; i++) {
            length_[i] = lengthTarget_[i];
            feedback_[i] = feedbackTarget_[i];
        }
        norm_ = normTarget_;
        dampCoef_ = dampTarget_;
        preDelay_ = preTarget_;
        primed_ = true;
    }
}

void Tail::step(int frames) {
    float k = 1.f - std::exp(-frames / (0.08f * fs_));
    for (int i = 0; i < Lines; i++) {
        length_[i] += (lengthTarget_[i] - length_[i]) * k;
        feedback_[i] += (feedbackTarget_[i] - feedback_[i]) * k;
    }
    norm_ += (normTarget_ - norm_) * k;
    dampCoef_ += (dampTarget_ - dampCoef_) * k;
    preDelay_ += (preTarget_ - preDelay_) * k;
}

void Tail::run(float in, float& left, float& right) {
    pre_.push(in);
    float x = pre_.read(preDelay_);
    float y[Lines];
    for (int i = 0; i < Lines; i++) {
        float raw = lines_[i].read(length_[i]);
        damp_[i] += (raw - damp_[i]) * dampCoef_;
        y[i] = damp_[i] * feedback_[i];
    }
    // an eight point hadamard in three butterfly passes, then scaled to keep the loop's energy
    float h[Lines];
    for (int i = 0; i < Lines; i++) h[i] = y[i];
    for (int span = 1; span < Lines; span <<= 1) {
        for (int i = 0; i < Lines; i += span * 2) {
            for (int j = i; j < i + span; j++) {
                float a = h[j], b = h[j + span];
                h[j] = a + b;
                h[j + span] = a - b;
            }
        }
    }
    constexpr float Scale = 0.35355339f;
    // a tiny offset keeps the decaying loop out of denormal numbers, which are slow on some cores
    for (int i = 0; i < Lines; i++) lines_[i].push(x + h[i] * Scale + 1e-20f);
    left = (y[0] - y[2] + y[4] - y[6] + y[1] * 0.5f) * norm_;
    right = (y[1] + y[3] - y[5] - y[7] + y[2] * 0.5f) * norm_;
}

void Tail::clear() {
    for (auto& line : lines_) line.clear();
    pre_.clear();
    for (float& d : damp_) d = 0.f;
}

}
