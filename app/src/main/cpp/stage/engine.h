// the 3d room: places every part of the song around a head in a box, with its echoes off the walls and the room's tail
#pragma once

#include <cstdint>
#include <memory>

#include "dsp.h"
#include "hrtf.h"
#include "splitter.h"
#include "tail.h"

namespace stage {

// the float array kotlin hands over (StageNative.kt builds the same layout)
enum Param {
    ParamEnabled, ParamMode, ParamRoomW, ParamRoomD, ParamRoomH, ParamAbsorb, ParamDamp, ParamRoomAmount,
    ParamListenerX, ParamListenerY, ParamListenerZ, ParamYaw, ParamMaster, ParamObjects,
};
// every kind of sound can be in the room up to four times; an object's slot is kind * Copies + copy
enum Kind { KindVocal, KindCenter, KindLeft, KindRight, KindAmbience, KindSub, KindCount };
constexpr int Copies = 4;
constexpr int ObjCount = KindCount * Copies;
// x, y, z in metres, linear gain, width
constexpr int ObjectStride = 5;
constexpr int ParamCount = ParamObjects + ObjCount * ObjectStride;
enum Mode { ModeHeadphones, ModeSpeakers };

class Engine {
public:
    Engine(int sampleRate, int channels);

    void setParams(const float* params, int count);

    // stereo out for every frame in; true while it's only passing the song through and could be skipped
    bool process(const int16_t* in, int16_t* out, int frames);

    // how loud each kind of sound is right now, 0..1, for the room's dots (KindCount values)
    void levels(float* out) const;

    void clear();

private:
    static constexpr int Block = 128;
    static constexpr int Walls = 6;

    struct Echo {
        float delay[2] = {}, gain[2] = {}, coef[2] = {1, 1}, state[2] = {};
        float delayStep[2] = {}, gainStep[2] = {};
    };

    // one placed copy of a band
    struct Voice {
        int dir = -1, oldDir = -1;
        float fade = 1.f;
        float gain[2] = {}, gainStep[2] = {};
        float air = 1.f, airState[2] = {};
        float send = 0.f, sendStep = 0.f;
        float pos[3] = {};
        bool placed = false;
        bool silent = true;
        Echo echoes[Walls];
    };

    // one part of the song: its past samples, shared by every copy placed in the room
    struct Band {
        Ring line;
        std::vector<float> hist;
        int histPos = 0;
        float level = 0.f, peak = 0.f;
        Voice copies[Copies];
    };

    struct SubOut {
        float pos[3] = {};
        bool placed = false;
        float gain[2] = {}, gainStep[2] = {}, delay[2] = {};
    };

    void block(const int16_t* in, int16_t* out, int frames);
    void renderWet(const int16_t* in, int frames);
    void renderDry(const int16_t* in, int frames);
    void readFrame(const int16_t* frame, float* bands, float& sub);
    void place(int frames);
    void placeVoice(Voice& voice, const Band& band, bool fromOneSide, const float* target, float userGain, int frames, bool headphones);
    void placeSub(SubOut& out, const float* target, float userGain, int frames, bool headphones);
    void toHead(const float* world, float* head, float& distance) const;
    void resetWet();

    int fs_, channels_;
    bool surround_;
    std::unique_ptr<Splitter> splitter_;
    std::shared_ptr<const HrtfSet> hrtf_;
    Tail tail_;
    Band bands_[BandCount];
    SubOut subs_[Copies];
    Ring subLine_;
    float subLevel_ = 0.f, subPeak_ = 0.f;
    Biquad lfe_[2];
    // which band each surround channel feeds, and how much
    int role_[8] = {};

    float target_[ParamCount] = {};
    float room_[3] = {4, 5, 2.7f}, listener_[4] = {}, master_ = 1.f, tailGain_ = 0.f;
    float lastRoom_[5] = {};
    bool roomPlaced_ = false;

    bool fresh_ = true, wet_ = false;
    int mode_ = ModeHeadphones;
    enum Phase { Steady, FadingOut, FadingIn } phase_ = Steady;
    float fadeGain_ = 1.f, fadeStep_;
    float dirFadeStep_;

    float mixL_[Block] = {}, mixR_[Block] = {};
};

}
