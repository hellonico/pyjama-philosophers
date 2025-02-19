(ns utils.voice
  (:require [clojure.java.shell :refer [sh]])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream File)
           (javax.sound.sampled AudioFileFormat$Type AudioFormat AudioInputStream AudioSystem DataLine$Info TargetDataLine)))

;; Audio format configuration
(def sample-rate 16000)                                     ; 16 kHz
(def sample-size-in-bits 8)
(def channels 1)
(def signed true)
(def big-endian true)

(def audio-format
  (AudioFormat. sample-rate sample-size-in-bits channels signed big-endian))

;; Silence detection parameters
(def silence-threshold 112)                                 ; Adjust based on your microphone sensitivity
(def silence-duration-ms 5000)                              ; Stop after 3 seconds of silence

;; Function to calculate the RMS (root mean square) of audio data
(defn calculate-rms [audio-bytes]
  (let [samples (map #(Math/abs %) (map #(- % 128) (map #(bit-and % 0xFF) audio-bytes)))]
    (Math/sqrt (/ (reduce + (map #(* % %) samples)) (count samples)))))



;; Function to record until silence is detected
(defn record-until-silence [output-file]
  (let [info (DataLine$Info. TargetDataLine audio-format)
        line (AudioSystem/getLine info)
        buffer-size 4096
        buffer (byte-array buffer-size)
        output-stream (ByteArrayOutputStream.)
        silence-start (atom nil)]

    ;; Ensure the microphone opens
    (when (instance? TargetDataLine line)
      (.open line)
      (.start line)
      (println "Recording started... Speak now!")

      ;; Recording loop
      (loop []
        (let [bytes-read (.read line buffer 0 (alength buffer))]
          (when (pos? bytes-read)                           ;; Avoid writing empty frames
            (.write output-stream buffer 0 bytes-read))

          (let [rms (calculate-rms buffer)]
            (println "RMS Value:" rms)

            (if (< rms silence-threshold)
              (do
                (reset! silence-start nil)                  ;; Reset silence detection
                (recur))
              (do
                (when (nil? @silence-start)
                  (reset! silence-start (System/currentTimeMillis)))

                (let [silence-elapsed (- (System/currentTimeMillis) @silence-start)]
                  (println "Silence duration:" silence-elapsed)
                  (if (>= silence-elapsed silence-duration-ms)
                    (do
                      (.stop line)
                      (.close line)
                      (println "Recording stopped due to silence.")

                      ;; Get recorded data
                      (let [byte-array (.toByteArray output-stream)]
                        (if (seq byte-array)                ;; Prevent writing empty files
                          (let [byte-input (ByteArrayInputStream. byte-array)
                                audio-input-stream (AudioInputStream. byte-input audio-format (/ (count byte-array) (.getFrameSize audio-format)))]
                            ;; Write to a valid WAV file
                            (AudioSystem/write audio-input-stream AudioFileFormat$Type/WAVE (File. output-file))
                            (println "Saved as:" output-file))
                          (println "Error: No audio recorded!"))))
                    (recur)))))))))))

;; Function to save recorded audio to a file
(defn save-audio-to-file [audio-bytes file-path]
  (with-open [out (java.io.FileOutputStream. file-path)]
    (.write out audio-bytes))
  (println (str "Audio saved to " file-path)))

;; Main function to record and save audio
(defn record-and-save [file-path]
  (let [audio-bytes (record-until-silence file-path)]
    (save-audio-to-file audio-bytes file-path)))

(def whisper-path "/opt/homebrew/bin/whisper-cpp")
(def model-path "./whisper.cpp/models/ggml-medium.bin")

(defn transcribe-audio [audio-file]
  (let [cmd [whisper-path "--model" model-path "--file" audio-file]
        result (apply sh cmd)]
    (:out result)))

;; Usage
(defn -main []
  (let [file-path "output.wav"]
    (record-and-save file-path)
    (transcribe-audio (str file-path))
    ))