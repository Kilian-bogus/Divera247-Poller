package de.netzkronehd.divera247.service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SoundService {
    private static final float SAMPLE_RATE = 44100.0f;

    public ToneHandle playInfo() {
        return playTone(buildInfoTone());
    }

    public ToneHandle playPriority() {
        return playTone(buildPriorityTone());
    }

    private ToneHandle playTone(byte[] audioData) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            ToneHandle handle = new ToneHandle(line, audioData);
            handle.start();
            return handle;
        } catch (LineUnavailableException exception) {
            throw new RuntimeException("Failed to play generated sound", exception);
        }
    }

    private byte[] buildInfoTone() {
        double[] frequencies = {660.0, 880.0, 1175.0};
        double segmentSeconds = 0.28;
        double silenceSeconds = 0.18;
        int toneSamples = (int) (SAMPLE_RATE * segmentSeconds * frequencies.length);
        int silenceSamples = (int) (SAMPLE_RATE * silenceSeconds);
        byte[] data = new byte[(toneSamples + silenceSamples) * 2];

        int sampleIndex = 0;
        for (double frequency : frequencies) {
            int segmentSamples = (int) (SAMPLE_RATE * segmentSeconds);
            for (int i = 0; i < segmentSamples; i++) {
                double local = i / SAMPLE_RATE;
                double envelope = envelope(local, segmentSeconds, 0.015, 0.05);
                short sample = sample(frequency, sampleIndex / SAMPLE_RATE, envelope * 0.38);
                writeSample(data, sampleIndex++, sample);
            }
        }

        return data;
    }

    private byte[] buildPriorityTone() {
        double durationSeconds = 1.25;
        int totalSamples = (int) (SAMPLE_RATE * durationSeconds);
        byte[] data = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double time = i / SAMPLE_RATE;
            double cycle = time % 0.38;
            if (cycle >= 0.23) {
                continue;
            }

            double frequency = cycle < 0.115 ? 740.0 : 988.0;
            double envelope = envelope(cycle, 0.23, 0.01, 0.04);
            double volume = envelope * envelope(time, durationSeconds, 0.015, 0.08) * 0.52;
            short sample = sample(frequency, time, volume);
            writeSample(data, i, sample);
        }

        return data;
    }

    private double envelope(double time, double duration, double attack, double release) {
        double attackVolume = Math.min(1.0, time / attack);
        double releaseVolume = Math.min(1.0, (duration - time) / release);
        return Math.max(0.0, Math.min(attackVolume, releaseVolume));
    }

    private short sample(double frequency, double time, double volume) {
        double value = Math.sin(2.0 * Math.PI * frequency * time) * volume;
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(value * Short.MAX_VALUE)));
    }

    private void writeSample(byte[] data, int sampleIndex, short sample) {
        int byteIndex = sampleIndex * 2;
        data[byteIndex] = (byte) (sample & 0xff);
        data[byteIndex + 1] = (byte) ((sample >> 8) & 0xff);
    }

    public static class ToneHandle implements AutoCloseable {
        private final SourceDataLine line;
        private final byte[] audioData;
        private volatile boolean running = true;
        private Thread thread;

        private ToneHandle(SourceDataLine line, byte[] audioData) {
            this.line = line;
            this.audioData = audioData;
        }

        private void start() {
            thread = new Thread(() -> {
                while (running) {
                    line.write(audioData, 0, audioData.length);
                }
                line.drain();
                line.stop();
                line.close();
            }, "divera-generated-tone");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void close() {
            running = false;
            line.flush();
        }
    }
}
