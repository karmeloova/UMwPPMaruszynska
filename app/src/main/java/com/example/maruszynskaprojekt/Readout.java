package com.example.maruszynskaprojekt;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class Readout extends Thread {

    // Konfiguracja kanału audio
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    // Wskaźniki do głównych komponentów
    double[] x, y, ampl;
    android.app.Activity activity; //wskaznik do głównego wątku (aktywność)
    FFT myFFT;
    MainActivity main; //wskaźnik do klasy głównego wątku

    //Flaga wyznaczająca czas życia wątku
    boolean shouldRun = true;

    //Konstruktor klasy
    public Readout(MainActivity _main)
    {
        // Przypisanie wartości z wątku MainActivity do zmiennych z wątku Reeadout

        main = _main;

        activity = _main;
        x = _main.x;
        y = _main.y;
        ampl = _main.amplitude;

        myFFT = new FFT(_main.blocksize);
    }

    @Override
    // Metoda chodząca na osobnym wątku
    public void run()
    {
        for (int i = 0; i < main.blocksize; i++) {
            y[i] = 0;
        }

        short[] audioBuffer = new short[main.blocksize]; //bufor do przechowywania danych (dźwiek)
        // Wielkosc bufora pobierana z ustawień androida
        int bufferSize = AudioRecord.getMinBufferSize(main.samplingFrequency,
                channelConfiguration,
                audioEncoding);

        // Sprawdzanie uprawnień do nagrywania audio
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    0);
            return;
        }
        // Obiekt umożliwiający przechywtywanie danych z mikforonu
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                main.samplingFrequency,
                channelConfiguration,
                audioEncoding,
                bufferSize);
        audioRecord.startRecording(); //uruchomienie
        // Wykonanie -> uśpienie -> wykonanie -> uśpienie
        // Główna pętla wątku
        while (shouldRun) {
            // Odczytywanie danych audio
            int bufferReadResult = audioRecord.read(audioBuffer, 0, main.blocksize);

            for (int i = 0; i < main.blocksize && i < bufferReadResult; i++) {
                x[i] = (double) audioBuffer[i] / 32768.0; // konwersja 16-bitowych danych na double
            }

            // Obliczenia
            int newReading = ComputeFFT();
            main.temperature = (main.a * ComputeAVG(newReading)) + main.b; // y = ax + b

            // Uśpienie wątku
            try {
                Thread.sleep(300); // 3 razy na sekundę
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        audioRecord.stop();
    }

    //Obliczanie amplitudy sygnału
    //Zwraca miejsce w którym osiągnięto "spike"
    private int ComputeFFT() {
        myFFT.fft(x, y);
        // Miejce, w którym jest największa wartość
        int peack = 0;
        // Największa wartość
        double maxValue = Double.MIN_VALUE;

        for (int i = 0; i < main.blocksize / 2; i++)
        {
            ampl[i] = x[i] * x[i] + y[i] * y[i];

            if (ampl[i] > maxValue) {
                maxValue = ampl[i];
                peack = i;
            }
        }
        // Normalizacja (aby było w zakresie, w którym chcemy)
        for (int i = 0; i < main.blocksize / 2; i++)
        {
            ampl[i] = (ampl[i] * 500) / maxValue;
        }
        // Zwracamy miejsce, w którym jest największa wartość
        return peack;
    }

    // Obliczanie średniej amplitudy z odczytów.
    private double ComputeAVG(int newReading)
    {
        //Przesuwanie pomiarów - średnia pełzająca (przesuwamy o 1 coś wychodzi i wchodzi na to coś innego)
        for (int i = 0; i < main.readings.length - 1; i++)
        {
            main.readings[i] = main.readings[i + 1];
        }
        main.readings[main.readings.length - 1] = newReading;

        //Wyznaczanie średniej
        double avg = 0;
        for (int i = 0; i < main.readings.length; i++)
        {
            avg += main.readings[i];
        }

        return (avg / main.readings.length);
    }
}