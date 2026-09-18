package io.github.alcapone11.sodiumfpsstepfix;

import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

public final class PreciseFrameLimiter {
    private static final int SAMPLE_COUNT = 16;
    private static final long ONE_SECOND_NANOS = 1_000_000_000L;
    private static final long MIN_PARK_NANOS = 100_000L;
    private static final long SPIN_SAFETY_BUFFER_NANOS = 500_000L;
    private static final long MAX_OVERSHOOT_SAMPLE_NANOS = 25_000_000L;
    private static final long MAX_AVERAGE_OVERSHOOT_NANOS = 2_000_000L;

    private final long[] overshootSamples = new long[SAMPLE_COUNT];
    private int sampleIndex;
    private int sampleCount;
    private int lastTargetFps;
    private long nextFrameTargetNanos;

    public void awaitNextFrame(int targetFps) {
        if (targetFps <= 0) {
            reset();
            return;
        }

        long now = System.nanoTime();
        long frameDurationNanos = ONE_SECOND_NANOS / targetFps;
        if (targetFps != this.lastTargetFps || this.nextFrameTargetNanos == 0L) {
            resetTiming(targetFps, now + frameDurationNanos);
        }

        long remainingNanos;
        while ((remainingNanos = this.nextFrameTargetNanos - System.nanoTime()) > 0L) {
            long averageOvershootNanos = averageOvershootNanos();
            long parkBudgetNanos = remainingNanos - averageOvershootNanos - SPIN_SAFETY_BUFFER_NANOS;
            if (parkBudgetNanos > MIN_PARK_NANOS) {
                long beforeParkNanos = System.nanoTime();
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    reset();
                    return;
                }

                LockSupport.parkNanos(parkBudgetNanos);
                long parkDurationNanos = System.nanoTime() - beforeParkNanos;
                long overshootNanos = parkDurationNanos - parkBudgetNanos;
                if (overshootNanos > 0L && overshootNanos < MAX_OVERSHOOT_SAMPLE_NANOS) {
                    recordOvershoot(overshootNanos);
                }
            } else {
                Thread.onSpinWait();
            }
        }

        now = System.nanoTime();
        this.nextFrameTargetNanos = Math.max(this.nextFrameTargetNanos + frameDurationNanos, now);
    }

    public void reset() {
        Arrays.fill(this.overshootSamples, 0L);
        this.sampleIndex = 0;
        this.sampleCount = 0;
        this.lastTargetFps = 0;
        this.nextFrameTargetNanos = 0L;
    }

    private void resetTiming(int targetFps, long nextFrameTargetNanos) {
        Arrays.fill(this.overshootSamples, 0L);
        this.sampleIndex = 0;
        this.sampleCount = 0;
        this.lastTargetFps = targetFps;
        this.nextFrameTargetNanos = nextFrameTargetNanos;
    }

    private void recordOvershoot(long overshootNanos) {
        this.overshootSamples[this.sampleIndex] = overshootNanos;
        this.sampleIndex = (this.sampleIndex + 1) % SAMPLE_COUNT;
        if (this.sampleCount < SAMPLE_COUNT) {
            this.sampleCount++;
        }
    }

    private long averageOvershootNanos() {
        if (this.sampleCount == 0) {
            return 0L;
        }

        long total = 0L;
        for (int i = 0; i < this.sampleCount; i++) {
            total += this.overshootSamples[i];
        }

        return Math.min(total / this.sampleCount, MAX_AVERAGE_OVERSHOOT_NANOS);
    }
}
