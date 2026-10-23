package com.example.myapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.animation.DecelerateInterpolator;
import android.animation.ValueAnimator;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private CompassView compassView;

    private TextView degreeText;
    private TextView directionText;
    private TextView statusText;

    private float azimuth = 0f;
    private boolean hasReading = false;

    private static final int BG = Color.rgb(11, 17, 29);
    private static final int DIAL = Color.rgb(29, 39, 61);
    private static final int RED = Color.rgb(255, 55, 70);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(160, 171, 194);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        sensorManager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);

        rotationSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(12), dp(4), dp(12), dp(8));

        TextView title = new TextView(this);
        title.setText("COMPASS");
        title.setTextColor(WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(43)));

        TextView subtitle = new TextView(this);
        subtitle.setText("البوصلة الرقمية");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(34)));

        degreeText = new TextView(this);
        degreeText.setText("--°");
        degreeText.setTextColor(WHITE);
        degreeText.setTextSize(46);
        degreeText.setGravity(Gravity.CENTER);
        degreeText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(degreeText, new LinearLayout.LayoutParams(-1, dp(63)));

        directionText = new TextView(this);
        directionText.setText("جارٍ تحديد الاتجاه");
        directionText.setTextColor(RED);
        directionText.setTextSize(23);
        directionText.setGravity(Gravity.CENTER);
        root.addView(directionText, new LinearLayout.LayoutParams(-1, dp(39)));

        // Fixed direction labels: they do NOT rotate with the compass.
        LinearLayout directions = new LinearLayout(this);
        directions.setOrientation(LinearLayout.VERTICAL);
        directions.setGravity(Gravity.CENTER);
        directions.setPadding(0, dp(4), 0, dp(4));

        TextView north = makeLabel("شمال ↑");
        directions.addView(north);

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.HORIZONTAL);
        middle.setGravity(Gravity.CENTER);
        TextView west = makeLabel("غرب");
        TextView east = makeLabel("شرق");

        middle.addView(west, new LinearLayout.LayoutParams(0, dp(35), 1));
        middle.addView(east, new LinearLayout.LayoutParams(0, dp(35), 1));
        directions.addView(middle, new LinearLayout.LayoutParams(-1, dp(35)));

        TextView south = makeLabel("جنوب ↓");
        directions.addView(south);

        root.addView(directions, new LinearLayout.LayoutParams(-1, dp(95)));

        compassView = new CompassView(this);
        LinearLayout.LayoutParams compassParams =
                new LinearLayout.LayoutParams(-1, 0, 1f);
        compassParams.topMargin = dp(2);
        compassParams.bottomMargin = dp(2);
        root.addView(compassView, compassParams);

        statusText = new TextView(this);
        statusText.setText("جارٍ تشغيل حساس الاتجاه...");
        statusText.setTextColor(MUTED);
        statusText.setTextSize(13);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, new LinearLayout.LayoutParams(-1, dp(35)));

        setContentView(root);

        if (rotationSensor == null) {
            statusText.setText(
                    "حساس الاتجاه غير متوفر؛ لا يمكن تحديد الاتجاه بدقة.");
        }
    }

    private TextView makeLabel(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(WHITE);
        view.setTextSize(15);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (rotationSensor != null) {
            boolean registered = sensorManager.registerListener(
                    this, rotationSensor, SensorManager.SENSOR_DELAY_UI);

            statusText.setText(registered
                    ? "حرّك الهاتف لمعرفة الاتجاه"
                    : "تعذر تشغيل حساس الاتجاه");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
            return;
        }

        float[] matrix = new float[9];
        float[] orientation = new float[3];

        SensorManager.getRotationMatrixFromVector(matrix, event.values);
        SensorManager.getOrientation(matrix, orientation);

        float newAzimuth = (float) Math.toDegrees(orientation[0]);
        newAzimuth = (newAzimuth + 360f) % 360f;

        if (!hasReading) {
            azimuth = newAzimuth;
            hasReading = true;
        } else {
            float diff = ((newAzimuth - azimuth + 540f) % 360f) - 180f;
            azimuth = (azimuth + diff * 0.25f + 360f) % 360f;
        }

        int degrees = Math.round(azimuth) % 360;

        degreeText.setText(String.format(Locale.US, "%03d°", degrees));
        directionText.setText(getDirection(degrees));

        compassView.setAzimuth(azimuth);
        statusText.setText("البوصلة تعمل");
    }

    private String getDirection(int degrees) {
        String[] names = {
                "شمال", "شمال شرقي", "شرق", "جنوب شرقي",
                "جنوب", "جنوب غربي", "غرب", "شمال غربي"
        };
        return names[Math.round(degrees / 45f) % 8];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (statusText == null) return;

        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            statusText.setText("الدقة منخفضة؛ حرّك الهاتف على شكل 8 للمعايرة");
        }
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private class CompassView extends View {

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float heading = 0f;

        CompassView(Context context) {
            super(context);
        }

        void setAzimuth(float value) {
            heading = value;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.43f;

            if (radius <= 0) return;

            p.setStyle(Paint.Style.FILL);
            p.setColor(DIAL);
            canvas.drawCircle(cx, cy, radius, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1.5f));
            p.setColor(Color.rgb(92, 108, 139));
            canvas.drawCircle(cx, cy, radius, p);

            // Rotate the dial and needle opposite to the phone heading.
            canvas.save();
            canvas.rotate(-heading, cx, cy);

            for (int angle = 0; angle < 360; angle += 5) {
                canvas.save();
                canvas.rotate(angle, cx, cy);

                boolean major = angle % 90 == 0;
                boolean medium = angle % 30 == 0;

                float inner = radius - (major ? dp(17)
                        : medium ? dp(11) : dp(5));

                p.setColor(major ? WHITE : MUTED);
                p.setStrokeWidth(major ? dp(2) : dp(1));
                p.setStyle(Paint.Style.STROKE);
                canvas.drawLine(cx, cy - inner,
                        cx, cy - radius + dp(2), p);

                // Cardinal letters rotate with the dial.
                if (major) {
                    String label;
                    if (angle == 0) label = "ش";
                    else if (angle == 90) label = "ق";
                    else if (angle == 180) label = "ج";
                    else label = "غ";

                    p.setStyle(Paint.Style.FILL);
                    p.setTextAlign(Paint.Align.CENTER);
                    p.setTextSize(dp(14));
                    p.setTypeface(Typeface.DEFAULT_BOLD);
                    p.setColor(angle == 0 ? RED : WHITE);
                    canvas.drawText(label, cx, cy - radius + dp(25), p);
                }

                canvas.restore();
            }

            // Red north needle.
            Path north = new Path();
            north.moveTo(cx, cy - radius * 0.84f);
            north.lineTo(cx - dp(7), cy);
            north.lineTo(cx, cy - dp(4));
            north.lineTo(cx + dp(7), cy);
            north.close();

            p.setStyle(Paint.Style.FILL);
            p.setColor(WHITE);
            canvas.drawPath(north, p);

            // White south needle.
            Path south = new Path();
            south.moveTo(cx, cy + radius * 0.84f);
            south.lineTo(cx - dp(7), cy);
            south.lineTo(cx, cy + dp(4));
            south.lineTo(cx + dp(7), cy);
            south.close();

            p.setColor(RED);
            canvas.drawPath(south, p);

            canvas.restore();

            // Center pivot.
            p.setColor(WHITE);
            canvas.drawCircle(cx, cy, dp(5), p);
            p.setColor(Color.rgb(11, 17, 29));
            canvas.drawCircle(cx, cy, dp(2), p);
        }
    }
}