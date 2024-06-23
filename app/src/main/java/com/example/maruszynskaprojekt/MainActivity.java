package com.example.maruszynskaprojekt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    int samplingFrequency = 12000; //Częstotliwość próbkowania [Hz]
    int blocksize = 2048; //Rozmiar bloku
    double[] x, y, amplitude; // Tablice do przechowywania danych

    // Parametry związane z odczytem temperatury
    int window = 50; //srednia na podstawie ostatnich 50 pomiarów
    double[] readings = new double[window];
    double temperature = 0;
    //Współczynniki funkcji liniowej służące do wyregulowania płytki
    double a = 0.1240;
    double b = -61.37;

    // Stan działania programu
    boolean isRunning = false;


    // Elementy interfejsu użytkownika
    ImageView chartImageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;
    TextView tempValue;
    Button startButton, stopButton;

    //Wielowątkowość
    MainActivity mainThread;
    Readout readoutProcess;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Wywołanie funkcji odpowiedzialnej za znalezienie elementów interfejsu
        // oraz przygotowanie pola do rysowania
        GetUIElements();
        mainThread = this;
        stopButton.setEnabled(false);

        startButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {
              ClickStartButton();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ClickStopButton();
            }
        });

        // Inicjalizacja tablic
        x = new double[blocksize];
        y = new double[blocksize];
        amplitude = new double[blocksize / 2];


        // Rysowanie wykresu
        DrawChart();
    }

    // Metoda rysująca wykres.
    public void DrawChart()
    {
        canvas.drawColor(Color.BLACK);

        // Rysowanie danych amplitudy na zielono
        paint.setColor(Color.GREEN);
        for (int i = 0; i < amplitude.length; i++) {
            int downy = 510;
            int upy = 510 - (int) amplitude[i];

            canvas.drawLine(i, downy, i, upy, paint);
        }

        // Rysowanie linii siatki na czerwono
        paint.setColor(Color.RED);
        for (int i = 0; i < amplitude.length; i++) {
            if (i % 10 == 0) {
                canvas.drawLine(i, 510, i, 500, paint);
            }
            if (i % 100 == 0) {
                canvas.drawLine(i, 510, i, 475, paint);
            }
        }

        // Aktualizacja wyświetlania temperatury
        tempValue.setText(String.format("%.2f", temperature) + " C");
        Refresh(500); // Odświeżanie wykresu co 500 ms
    }

    // Metoda odświeżająca wykres po określonym czasie
    // Działa na zasadzie, ze handler przytrzymuje 500 ms, po tym czasie uruchamia DrawChart()
    // Potem ponownie handler przytrzymuje na 500 ms i tak w kółko
    public void Refresh(int millis) {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                DrawChart();
            }
        };
        handler.postDelayed(runnable, millis);
    }

    public void GetUIElements() {
        // Znajdź elementy interfejsu w układzie
        tempValue = findViewById(R.id.tempOutputFinal);

        chartImageView = findViewById(R.id.wykres);
        bitmap = Bitmap.createBitmap(blocksize / 2, 520, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        chartImageView.setImageBitmap(bitmap);
        canvas.drawColor(Color.RED);

        startButton = findViewById(R.id.ButtonStart);
        stopButton = findViewById(R.id.ButtonStop);
    }

    public void ClickStartButton() {
        // Po wciśnięciu start blokuje się możliwość wystartowania

       if(!isRunning) {
           startButton.setEnabled(false);
           stopButton.setEnabled(true);
           readoutProcess = new Readout(mainThread);
           readoutProcess.start();
           isRunning = true;
       }

    }

    public void ClickStopButton() {
        // Informacja o tym, że proces odczytywania powinien zakończyć swoją działalność
        // (zabicie wątku)
        if(isRunning) {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            readoutProcess.shouldRun = false;
            canvas.drawColor(Color.BLACK);
            isRunning = false;
        }
        // Po wciśnięciu stop mamy możliwość ponownego uruchomienia

    }
}