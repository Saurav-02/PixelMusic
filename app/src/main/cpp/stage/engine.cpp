#include "engine.h"

#include <cstring>

namespace stage {

namespace {
// a source this far away plays at the song's own level
constexpr float RefDistance = 2.f;
// bass in a room hardly fades over a few metres, so a sub keeps its level further out
constexpr float SubRefDistance = 3.f;
constexpr float HeadRadiusSeconds = 0.00066f;
constexpr float EchoLevel = 0.7f;
constexpr float SendLevel = 0.35f;
constexpr float TailLevel = 0.55f;
constexpr float Knee = 0.9f;
constexpr float OneSideTrim = 0.7071f;

enum Role { RoleLeft, RoleRight, RoleVocal, RoleLfe, RoleAmbL, RoleAmbR, RoleBack };

// android's channel order for each count (fl fr fc lfe bl br sl sr, fewer channels drop from the end)
void rolesFor(int channels, int* roles) {
    static const int three[] = {RoleLeft, RoleRight, RoleVocal};
    static const int four[] = {RoleLeft, RoleRight, RoleAmbL, RoleAmbR};
    static const int five[] = {RoleLeft, RoleRight, RoleVocal, RoleAmbL, RoleAmbR};
    static const int six[] = {RoleLeft, RoleRight, RoleVocal, RoleLfe, RoleAmbL, RoleAmbR};
    static const int seven[] = {RoleLeft, RoleRight, RoleVocal, RoleLfe, RoleBack, RoleAmbL, RoleAmbR};
    static const int eight[] = {RoleLeft, RoleRight, RoleVocal, RoleLfe, RoleAmbL, RoleAmbR, RoleAmbL, RoleAmbR};
    const int* table = channels == 3 ? three : channels == 4 ? four : channels == 5 ? five
        : channels == 6 ? six : channels == 7 ? seven : eight;
    for (int c = 0; c < channels && c < 8; c++) roles[c] = table[c];
}

inline void dot2(const float* __restrict a, const float* __restrict b, const float* __restrict x, int n, float& outA, float& outB) {
// letting the sums reorder is what lets the compiler use simd here, almost three times faster
#pragma clang fp reassociate(on) contract(fast)
    float sumA = 0, sumB = 0;
    for (int i = 0; i < n; i++) {
        sumA += a[i] * x[i];
        sumB += b[i] * x[i];
    }
    outA = sumA;
    outB = sumB;
}

// gains for a plain stereo pair: centre plays full on both sides, 30 degrees off is already all one side
void panGains(float lateral, float& left, float& right) {
    float pan = std::min(1.f, std::max(-1.f, 2.f * lateral));
    left = std::min(1.f, 1.f - pan);
    right = std::min(1.f, 1.f + pan);
}

int16_t toPcm(float sample) {
    float a = std::fabs(sample);
    if (a > Knee) {
        float shaped = Knee + (1.f - Knee) * std::tanh((a - Knee) / (1.f - Knee));
        sample = sample < 0 ? -shaped : shaped;
    }
    float scaled = std::round(sample * 32767.f);
    return static_cast<int16_t>(std::min(32767.f, std::max(-32768.f, scaled)));
}
}

Engine::Engine(int sampleRate, int channels)
    : fs_(sampleRate), channels_(channels), surround_(channels > 2), tail_(static_cast<float>(sampleRate)) {
    if (!surround_) splitter_ = std::make_unique<Splitter>(sampleRate);
    else rolesFor(channels, role_);
    float fs = static_cast<float>(sampleRate);
    for (auto& band : bands_) band.line.init(static_cast<int>(0.35f * fs));
    subLine_.init(static_cast<int>(0.01f * fs) + 64);
    for (auto& f : lfe_) f.lowpass(fs, 120.f);
    fadeStep_ = 1.f / (0.012f * fs);
    dirFadeStep_ = 1.f / Block;
}

void Engine::setParams(const float* params, int count) {
    if (count != ParamCount) return;
    std::memcpy(target_, params, sizeof(target_));
}

bool Engine::process(const int16_t* in, int16_t* out, int frames) {
    int done = 0;
    while (done < frames) {
        int n = std::min(Block, frames - done);
        block(in + static_cast<size_t>(done) * channels_, out + static_cast<size_t>(done) * 2, n);
        done += n;
    }
    return !wet_ && phase_ == Steady && target_[ParamEnabled] < 0.5f;
}

void Engine::block(const int16_t* in, int16_t* out, int frames) {
    bool want = target_[ParamEnabled] > 0.5f;
    int mode = target_[ParamMode] > 0.5f ? ModeSpeakers : ModeHeadphones;
    if (fresh_) {
        // the start of a song or a seek: no fade, straight into whatever is wanted
        fresh_ = false;
        wet_ = want;
        mode_ = mode;
        if (wet_) resetWet();
    } else if (phase_ == Steady && (want != wet_ || (wet_ && mode != mode_))) {
        phase_ = FadingOut;
    }

    if (wet_) renderWet(in, frames);
    else renderDry(in, frames);

    for (int i = 0; i < frames; i++) {
        if (phase_ == FadingOut) fadeGain_ = std::max(0.f, fadeGain_ - fadeStep_);
        else if (phase_ == FadingIn) {
            fadeGain_ = std::min(1.f, fadeGain_ + fadeStep_);
            if (fadeGain_ >= 1.f) phase_ = Steady;
        }
        out[i * 2] = toPcm(mixL_[i] * fadeGain_);
        out[i * 2 + 1] = toPcm(mixR_[i] * fadeGain_);
    }

    // silent for a moment, so the switch itself is never heard as a click
    if (phase_ == FadingOut && fadeGain_ <= 0.f) {
        if (want && !wet_) resetWet();
        wet_ = want;
        mode_ = mode;
        phase_ = FadingIn;
    }
}

void Engine::renderDry(const int16_t* in, int frames) {
    constexpr float Scale = 1.f / 32768.f;
    if (!surround_) {
        for (int i = 0; i < frames; i++) {
            mixL_[i] = in[i * 2] * Scale;
            mixR_[i] = in[i * 2 + 1] * Scale;
        }
        return;
    }
    // the plain fold-down a surround song gets while the room is off
    for (int i = 0; i < frames; i++) {
        const int16_t* frame = in + static_cast<size_t>(i) * channels_;
        float l = 0, r = 0;
        for (int c = 0; c < channels_; c++) {
            float x = frame[c] * Scale;
            switch (role_[c]) {
                case RoleLeft: l += x; break;
                case RoleRight: r += x; break;
                case RoleVocal: l += 0.707f * x; r += 0.707f * x; break;
                case RoleAmbL: l += 0.707f * x; break;
                case RoleAmbR: l += 0.707f * x; break;
                case RoleBack: l += 0.5f * x; r += 0.5f * x; break;
                default: break;
            }
        }
        mixL_[i] = l * 0.55f;
        mixR_[i] = r * 0.55f;
    }
}

void Engine::readFrame(const int16_t* frame, float* bands, float& sub) {
    constexpr float Scale = 1.f / 32768.f;
    if (!surround_) {
        splitter_->push(frame[0] * Scale, frame[1] * Scale, bands, sub);
        return;
    }
    // a surround song already comes apart: its own channels become the objects
    for (int b = 0; b < BandCount; b++) bands[b] = 0.f;
    float lfe = 0.f;
    for (int c = 0; c < channels_; c++) {
        float x = frame[c] * Scale;
        switch (role_[c]) {
            case RoleLeft: bands[BandLeft] += x; break;
            case RoleRight: bands[BandRight] += x; break;
            case RoleVocal: bands[BandVocal] += x; break;
            case RoleLfe: lfe += x; break;
            case RoleAmbL: bands[BandAmbL] += x; break;
            case RoleAmbR: bands[BandAmbR] += x; break;
            case RoleBack: bands[BandAmbL] += 0.707f * x; bands[BandAmbR] += 0.707f * x; break;
            default: break;
        }
    }
    sub = lfe_[1].run(lfe_[0].run(lfe)) * 1.4f;
}

void Engine::renderWet(const int16_t* in, int frames) {
    if (!hrtf_) {
        // the asset loads in the background, so the first blocks after a cold start can come before it
        hrtf_ = hrtfFor(fs_);
        if (hrtf_) {
            for (auto& band : bands_) {
                band.hist.assign(static_cast<size_t>(hrtf_->taps) * 2 + 2, 0.f);
                band.histPos = 0;
                for (auto& v : band.copies) v.dir = -1;
            }
        }
    }
    place(frames);
    bool headphones = mode_ == ModeHeadphones && hrtf_;
    const HrtfSet* set = hrtf_.get();
    int taps = set ? set->taps : 0;
    float bands[BandCount];

    for (int i = 0; i < frames; i++) {
        float sub = 0.f;
        readFrame(in + static_cast<size_t>(i) * channels_, bands, sub);
        float l = 0.f, r = 0.f, send = 0.f;

        for (int b = 0; b < BandCount; b++) {
            Band& band = bands_[b];
            float x = bands[b];
            band.line.push(x);
            if (headphones) {
                band.hist[band.histPos] = x;
                band.hist[band.histPos + taps] = x;
            }
            const float* history = headphones ? band.hist.data() + band.histPos + 1 : nullptr;
            if (headphones) band.histPos = band.histPos + 1 == taps ? 0 : band.histPos + 1;
            band.peak = std::max(band.peak, std::fabs(x));

            for (auto& v : band.copies) {
                if (v.silent) continue;
                float yl = x, yr = x;
                if (headphones) {
                    dot2(set->leftOf(v.dir), set->rightOf(v.dir), history, taps, yl, yr);
                    if (v.fade < 1.f) {
                        float ol, orr;
                        dot2(set->leftOf(v.oldDir), set->rightOf(v.oldDir), history, taps, ol, orr);
                        yl = ol + (yl - ol) * v.fade;
                        yr = orr + (yr - orr) * v.fade;
                        v.fade = std::min(1.f, v.fade + dirFadeStep_);
                    }
                }
                v.gain[0] += v.gainStep[0];
                v.gain[1] += v.gainStep[1];
                v.airState[0] += (yl * v.gain[0] - v.airState[0]) * v.air;
                v.airState[1] += (yr * v.gain[1] - v.airState[1]) * v.air;
                l += v.airState[0];
                r += v.airState[1];

                for (auto& e : v.echoes) {
                    for (int ear = 0; ear < 2; ear++) {
                        float s = band.line.read(e.delay[ear]) * e.gain[ear];
                        e.state[ear] += (s - e.state[ear]) * e.coef[ear];
                        e.delay[ear] += e.delayStep[ear];
                        e.gain[ear] += e.gainStep[ear];
                    }
                    l += e.state[0];
                    r += e.state[1];
                }
                v.send += v.sendStep;
                send += x * v.send;
            }
        }

        subLine_.push(sub);
        subPeak_ = std::max(subPeak_, std::fabs(sub));
        for (auto& s : subs_) {
            if (!s.placed) continue;
            s.gain[0] += s.gainStep[0];
            s.gain[1] += s.gainStep[1];
            l += subLine_.read(s.delay[0]) * s.gain[0];
            r += subLine_.read(s.delay[1]) * s.gain[1];
        }

        float tl, tr;
        tail_.run(send, tl, tr);
        mixL_[i] = (l + tl * tailGain_) * master_;
        mixR_[i] = (r + tr * tailGain_) * master_;
    }
}

void Engine::toHead(const float* world, float* head, float& distance) const {
    float vx = world[0] - listener_[0], vy = world[1] - listener_[1], vz = world[2] - listener_[2];
    float yaw = listener_[3] * Pi / 180.f, c = std::cos(yaw), s = std::sin(yaw);
    float hx = vx * c - vz * s, hz = vx * s + vz * c;
    distance = std::max(0.2f, std::sqrt(hx * hx + vy * vy + hz * hz));
    head[0] = hx / distance;
    head[1] = vy / distance;
    head[2] = hz / distance;
}

void Engine::place(int frames) {
    const float* p = target_;
    float k = 1.f - std::exp(-frames / (0.06f * fs_));
    float roomTarget[3] = {std::max(1.f, p[ParamRoomW]), std::max(1.f, p[ParamRoomD]), std::max(1.f, p[ParamRoomH])};
    float listenerTarget[4] = {p[ParamListenerX], p[ParamListenerY], p[ParamListenerZ], p[ParamYaw]};
    if (!roomPlaced_) {
        std::memcpy(room_, roomTarget, sizeof(room_));
        std::memcpy(listener_, listenerTarget, sizeof(listener_));
        master_ = p[ParamMaster];
        roomPlaced_ = true;
    }
    for (int i = 0; i < 3; i++) room_[i] += (roomTarget[i] - room_[i]) * k;
    for (int i = 0; i < 3; i++) listener_[i] += (listenerTarget[i] - listener_[i]) * k;
    // the shortest way round, so turning from 170 to -170 degrees doesn't spin the long way
    float turn = std::remainder(listenerTarget[3] - listener_[3], 360.f);
    listener_[3] += turn * k;
    master_ += (p[ParamMaster] - master_) * k;
    tailGain_ += (p[ParamRoomAmount] * TailLevel - tailGain_) * k;

    float roomNow[5] = {room_[0], room_[1], room_[2], p[ParamAbsorb], p[ParamDamp]};
    bool moved = false;
    for (int i = 0; i < 5; i++) moved |= std::fabs(roomNow[i] - lastRoom_[i]) > 0.01f;
    if (moved) {
        tail_.setRoom(roomNow[0], roomNow[1], roomNow[2], roomNow[3], roomNow[4]);
        std::memcpy(lastRoom_, roomNow, sizeof(lastRoom_));
    }
    tail_.step(frames);

    bool headphones = mode_ == ModeHeadphones && hrtf_;
    for (int b = 0; b < BandCount; b++) {
        int kind = b <= BandRight ? b : KindAmbience;
        // a part that came from one channel (a side or half the ambience) now reaches both ears
        bool oneSide = b != BandVocal && b != BandCenter;
        for (int c = 0; c < Copies; c++) {
            const float* o = p + ParamObjects + (kind * Copies + c) * ObjectStride;
            float target[3] = {o[0], o[1], o[2]};
            if (b == BandAmbL || b == BandAmbR) {
                // the ambience is a pair, spread sideways from its dot by its width
                float spread = o[4] * std::min(3.f, room_[0] * 0.35f) * (b == BandAmbL ? -1.f : 1.f);
                target[0] = std::min(room_[0] / 2, std::max(-room_[0] / 2, o[0] + spread));
            }
            placeVoice(bands_[b].copies[c], bands_[b], oneSide, target, o[3], frames, headphones);
        }
    }
    for (int c = 0; c < Copies; c++) {
        const float* o = p + ParamObjects + (KindSub * Copies + c) * ObjectStride;
        placeSub(subs_[c], o, o[3], frames, headphones);
    }
    for (auto& band : bands_) {
        band.level = std::max(band.peak, band.level * 0.93f);
        band.peak = 0.f;
    }
    subLevel_ = std::max(subPeak_, subLevel_ * 0.93f);
    subPeak_ = 0.f;
}

void Engine::placeVoice(Voice& v, const Band& band, bool fromOneSide, const float* target, float userGain, int frames, bool headphones) {
    // a copy that isn't in the room costs nothing
    if (userGain <= 0.f && !v.placed) {
        v.silent = true;
        return;
    }
    bool fresh = !v.placed;
    float k = 1.f - std::exp(-frames / (0.06f * fs_));
    for (int i = 0; i < 3; i++) v.pos[i] = fresh ? target[i] : v.pos[i] + (target[i] - v.pos[i]) * k;
    float head[3], distance;
    toHead(v.pos, head, distance);
    float near = std::min(2.5f, std::max(0.06f, RefDistance / distance));
    float inv = 1.f / frames;

    float direct[2];
    if (headphones) {
        // a part that came from one channel now reaches both ears; without the trim it doubles up and the mix turns boxy
        float ears = fromOneSide ? OneSideTrim : 1.f;
        direct[0] = direct[1] = userGain * near * ears;
        int dir = hrtf_->nearest(head[0], head[1], head[2]);
        if (v.dir < 0 || fresh) {
            v.dir = dir;
            v.fade = 1.f;
        } else if (dir != v.dir && v.fade >= 1.f) {
            // a new direction crossfades in over one block; a further move waits for the next
            v.oldDir = v.dir;
            v.dir = dir;
            v.fade = 0.f;
        }
    } else {
        panGains(head[0], direct[0], direct[1]);
        float behind = head[2] < 0 ? 1.f + 0.3f * head[2] : 1.f;
        direct[0] *= userGain * near * behind;
        direct[1] *= userGain * near * behind;
    }
    // air only dulls what's further than the reference distance, so the default layout keeps the song's top end
    v.air = onePole(static_cast<float>(fs_), 22000.f / (1.f + std::max(0.f, distance - RefDistance) / 10.f));

    float absorb = target_[ParamAbsorb], damp = target_[ParamDamp], amount = target_[ParamRoomAmount];
    float reflect = std::sqrt(std::max(0.f, 1.f - absorb));
    float wallCut = 16000.f - 12500.f * damp;
    float maxDelay = static_cast<float>(band.line.size() - 8);
    float w = room_[0], d = room_[1], h = room_[2];
    for (int wall = 0; wall < Walls; wall++) {
        // the source mirrored in one wall: its echo arrives from there, as far away as the mirror image
        float image[3] = {v.pos[0], v.pos[1], v.pos[2]};
        switch (wall) {
            case 0: image[0] = w - image[0]; break;
            case 1: image[0] = -w - image[0]; break;
            case 2: image[1] = -image[1]; break;
            case 3: image[1] = 2 * h - image[1]; break;
            case 4: image[2] = d - image[2]; break;
            default: image[2] = -d - image[2]; break;
        }
        float dir[3], far;
        toHead(image, dir, far);
        float delay = std::min(maxDelay, std::max(0.f, (far - distance) / SpeedOfSound * fs_));
        float gain = reflect * std::min(1.5f, RefDistance / far) * userGain * amount * EchoLevel;
        float delays[2] = {delay, delay}, gains[2], cuts[2] = {wallCut, wallCut};
        if (headphones) {
            // the far ear hears it a little later, quieter and duller, the way a head shades it
            float side = std::fabs(dir[0]);
            int farEar = dir[0] >= 0 ? 0 : 1;
            delays[farEar] = std::min(maxDelay, delay + HeadRadiusSeconds * fs_ * side);
            gains[0] = gains[1] = gain;
            gains[farEar] *= 1.f - 0.45f * side;
            cuts[farEar] *= 1.f - 0.6f * side;
        } else {
            panGains(dir[0], gains[0], gains[1]);
            gains[0] *= gain;
            gains[1] *= gain;
        }
        Echo& e = v.echoes[wall];
        for (int ear = 0; ear < 2; ear++) {
            e.coef[ear] = onePole(static_cast<float>(fs_), cuts[ear]);
            // a copy coming in starts from nothing and rises over the block, so adding one never clicks
            if (fresh) {
                e.delay[ear] = delays[ear];
                e.gain[ear] = 0.f;
            }
            e.delayStep[ear] = (delays[ear] - e.delay[ear]) * inv;
            e.gainStep[ear] = (gains[ear] - e.gain[ear]) * inv;
        }
    }

    float send = userGain * near * amount * SendLevel;
    if (fresh) {
        v.gain[0] = v.gain[1] = 0.f;
        v.send = 0.f;
        for (auto& e : v.echoes) e.state[0] = e.state[1] = 0.f;
        v.airState[0] = v.airState[1] = 0.f;
    }
    v.gainStep[0] = (direct[0] - v.gain[0]) * inv;
    v.gainStep[1] = (direct[1] - v.gain[1]) * inv;
    v.sendStep = (send - v.send) * inv;
    v.placed = true;
    // muted or removed and already faded down: skipped, and placed afresh when it comes back
    v.silent = userGain <= 0.f && v.gain[0] <= 1e-5f && v.gain[1] <= 1e-5f;
    if (v.silent) v.placed = false;
}

void Engine::placeSub(SubOut& s, const float* target, float userGain, int frames, bool headphones) {
    float inv = 1.f / frames;
    if (userGain <= 0.f && !s.placed) return;
    float k = 1.f - std::exp(-frames / (0.06f * fs_));
    bool fresh = !s.placed;
    for (int i = 0; i < 3; i++) s.pos[i] = fresh ? target[i] : s.pos[i] + (target[i] - s.pos[i]) * k;
    float head[3], distance;
    toHead(s.pos, head, distance);
    float near = std::min(2.f, std::max(0.1f, SubRefDistance / distance));
    float gains[2];
    if (headphones) {
        // bass bends round the head, so only the arrival time and a touch of level give it a side
        float side = std::fabs(head[0]);
        int farEar = head[0] >= 0 ? 0 : 1;
        gains[0] = gains[1] = userGain * near;
        gains[farEar] *= 1.f - 0.25f * side;
        s.delay[farEar] = HeadRadiusSeconds * fs_ * side;
        s.delay[1 - farEar] = 0.f;
    } else {
        panGains(head[0] * 0.6f, gains[0], gains[1]);
        gains[0] *= userGain * near;
        gains[1] *= userGain * near;
        s.delay[0] = s.delay[1] = 0.f;
    }
    if (fresh) {
        s.gain[0] = 0.f;
        s.gain[1] = 0.f;
    }
    s.gainStep[0] = (gains[0] - s.gain[0]) * inv;
    s.gainStep[1] = (gains[1] - s.gain[1]) * inv;
    s.placed = true;
    // a removed sub fades out over this block, then stops being worked on
    if (userGain <= 0.f && s.gain[0] <= 1e-5f && s.gain[1] <= 1e-5f) s.placed = false;
}

void Engine::levels(float* out) const {
    for (int b = 0; b <= BandRight; b++) out[b] = std::min(1.f, bands_[b].level);
    out[KindAmbience] = std::min(1.f, std::max(bands_[BandAmbL].level, bands_[BandAmbR].level));
    out[KindSub] = std::min(1.f, subLevel_);
}

void Engine::resetWet() {
    if (splitter_) splitter_->clear();
    int taps = hrtf_ ? hrtf_->taps : 0;
    for (auto& band : bands_) {
        band.line.clear();
        band.hist.assign(static_cast<size_t>(taps) * 2 + 2, 0.f);
        band.histPos = 0;
        for (auto& v : band.copies) {
            v.dir = -1;
            v.fade = 1.f;
            v.placed = false;
            v.silent = true;
            v.gain[0] = v.gain[1] = 0.f;
            v.airState[0] = v.airState[1] = 0.f;
            for (auto& e : v.echoes) e.state[0] = e.state[1] = 0.f;
        }
    }
    for (auto& s : subs_) s.placed = false;
    subLine_.clear();
    for (auto& f : lfe_) f.clear();
    tail_.clear();
    roomPlaced_ = false;
}

void Engine::clear() {
    fresh_ = true;
    wet_ = false;
    phase_ = Steady;
    fadeGain_ = 1.f;
    resetWet();
}

}
