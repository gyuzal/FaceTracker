/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.s8launch.cheil.facetracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.s8launch.cheil.facetracker.R;

import java.util.Random;

class HatsRecord{
    public int path;
    public float width;
    public float ratioX;
    public float ratioY;

    public float lastX = 0;
    public float lastY = 0;
    public float lastAngle = 0;
    public float lastWidth = 0;

    HatsRecord(int path, float width, float ratioX, float ratioY){
        this.path = path;
        this.width = width;
        this.ratioX = ratioX;
        this.ratioY = ratioY;
    }
}

/**
 * Factory for creating a tracker and associated graphic to be associated with a new face.  The
 * multi-processor uses this factory to create face trackers as needed -- one for each individual.
 */
class FaceTrackerFactory implements MultiProcessor.Factory<Face> {
    private GraphicOverlay mGraphicOverlay;
    private MultiTrackerActivity context;
    FaceTrackerFactory(MultiTrackerActivity context, GraphicOverlay graphicOverlay) {
        mGraphicOverlay = graphicOverlay;
        this.context = context;
    }

    @Override
    public Tracker<Face> create(Face face) {
        FaceGraphic graphic = new FaceGraphic(this.context, mGraphicOverlay);
        return new GraphicTracker<>(mGraphicOverlay, graphic);
    }
}

/**
 * Graphic instance for rendering face position, size, and ID within an associated graphic overlay
 * view.
 */
class FaceGraphic extends TrackedGraphic<Face> {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
            Color.MAGENTA,
            Color.RED,
            Color.YELLOW
    };

    private static final HatsRecord HATS_CHOICES[] = {
            new HatsRecord(R.drawable.hat, 1.3f, 0.51f, 1.125f),
            new HatsRecord(R.drawable.hat2, 1.0f, 0.51f, 1.0f),
            new HatsRecord(R.drawable.hat3, 1.2f, 0.4f, 0.75f),
            new HatsRecord(R.drawable.hat4, 1.9f, 0.5f, 1.0f),
            new HatsRecord(R.drawable.hat5, 1.5f, 0.5f, 1.1f),
            new HatsRecord(R.drawable.hat6, 1.0f, 0.6f, 1.15f),
            new HatsRecord(R.drawable.hat7, 1.1f, 0.65f, 1.1f),
            new HatsRecord(R.drawable.hat8, 1.2f, 0.6f, 1.0f),
            new HatsRecord(R.drawable.hat9, 1.5f, 0.5f, 1.1f),
            new HatsRecord(R.drawable.hat10, 1.0f, 0.5f, 1.1f),
            new HatsRecord(R.drawable.hat11, 1.3f, 0.5f, 1.0f),
            new HatsRecord(R.drawable.hat12, 1.75f, 0.5f, 0.7f),
            new HatsRecord(R.drawable.hat13, 1.0f, 0.5f, 1.2f),
            new HatsRecord(R.drawable.hat14, 1.0f, 0.5f, 1.25f),
    };
    private static final HatsRecord GLASS_CHOICES[] = {
            new HatsRecord(R.drawable.gla1, 0.8f, 0.5f, 0.5f),
            new HatsRecord(R.drawable.gla2, 0.8f, 0.5f, 0.5f),
            new HatsRecord(R.drawable.gla3, 0.8f, 0.5f, 0.5f),
            new HatsRecord(R.drawable.gla4, 0.8f, 0.5f, 0.4f),
            new HatsRecord(R.drawable.gla5, 0.8f, 0.5f, 0.4f),
            new HatsRecord(R.drawable.gla6, 0.8f, 0.5f, 0.4f),
            new HatsRecord(R.drawable.gla8, 0.8f, 0.5f, 0.4f),
            new HatsRecord(R.drawable.gla9, 0.8f, 0.5f, 0.4f),
            new HatsRecord(R.drawable.gla10, 1.0f, 0.6f, 0.4f),
            new HatsRecord(R.drawable.gla12, 0.8f, 0.5f, 0.4f),
            new HatsRecord(R.drawable.gla13, 0.8f, 0.5f, 0.35f),
            new HatsRecord(R.drawable.gla14, 0.8f, 0.5f, 0.35f),
            new HatsRecord(R.drawable.gla15, 0.8f, 0.5f, 0.35f),
            new HatsRecord(R.drawable.gla16, 0.8f, 0.5f, 0.35f),
    };
    private static final HatsRecord BE_CHOICES[] = {
            new HatsRecord(R.drawable.be1, 0.8f, 0.52f, -0.2f),
            new HatsRecord(R.drawable.be2, 0.8f, 0.52f, -0.28f),
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private Paint paint;
    private Bitmap bitmapHats, bitmapGlass, bitmapBe;

    private volatile Face mFace;
    private HatsRecord selectedHats;
    private HatsRecord selectedGlass;
    private HatsRecord selectedBe;
    private MultiTrackerActivity context;

    FaceGraphic(MultiTrackerActivity context, GraphicOverlay overlay) {
        super(overlay);
        this.context = context;
        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

//
        Random r = new Random();
        selectedHats = HATS_CHOICES[r.nextInt(HATS_CHOICES.length)];
        bitmapHats = BitmapFactory.decodeResource(overlay.getContext().getResources(), selectedHats.path);

        if(r.nextInt(2) == 1) {
            selectedGlass = GLASS_CHOICES[r.nextInt(GLASS_CHOICES.length)];
            bitmapGlass = BitmapFactory.decodeResource(overlay.getContext().getResources(), selectedGlass.path);
        }
        if(r.nextInt(4) == 1) {
            selectedBe = BE_CHOICES[r.nextInt(BE_CHOICES.length)];
            bitmapBe = BitmapFactory.decodeResource(overlay.getContext().getResources(), selectedBe.path);
        }

    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateItem(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position, size, and ID on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float cx = Math.round(translateX(face.getPosition().x + face.getWidth() / 2));
        float cy = Math.round(translateY(face.getPosition().y + face.getHeight() / 2));
        cx = Math.round((selectedHats.lastX+cx)/2);
        cy = Math.round((selectedHats.lastY+cy)/2);
        // canvas.drawCircle(cx, cy, FACE_POSITION_RADIUS, mFacePositionPaint);
        //canvas.drawText("id: " + getId(), cx + ID_X_OFFSET, cy + ID_Y_OFFSET, mIdPaint);
        float faceWidth = Math.round(face.getWidth());
//        if (Math.abs(Math.abs(selectedHats.lastWidth) - Math.abs(faceWidth)) < 10) {
//            faceWidth = selectedHats.lastWidth;
//        }
        float rotate = Math.round(translateAngle(face.getEulerZ()));
//        if (Math.abs(Math.abs(selectedHats.lastAngle) - Math.abs(rotate)) < 5){
//            rotate = selectedHats.lastAngle;
//        }
        // =============== BE RENDER ======================= //
        if (bitmapBe != null){
            float ratio3 = (float) bitmapBe.getHeight() / (float) bitmapBe.getWidth();
            float w3 = faceWidth * selectedBe.width;
            float h3 = w3 * ratio3;
            Bitmap newimage = Bitmap.createScaledBitmap(bitmapBe, (int) w3, (int) h3, true);
            Matrix mat = new Matrix();
            mat.reset();
            float dx = cx - (newimage.getWidth()*selectedBe.ratioX);
            float dy = cy  - (newimage.getHeight()*selectedBe.ratioY);

            mat.postTranslate(dx, dy);
            mat.postRotate(rotate, cx, cy);
            canvas.drawBitmap(newimage, mat, paint);
        }
        // ================ GLASS RENDER ===================== //
        if (bitmapGlass != null) {
            float ratio2 = (float) bitmapGlass.getHeight() / (float) bitmapGlass.getWidth();
            float w2 = faceWidth * selectedGlass.width;
            float h2 = w2 * ratio2;
            Bitmap newimage = Bitmap.createScaledBitmap(bitmapGlass, (int) w2, (int) h2, true);

            Matrix mat = new Matrix();
            mat.reset();
            float dx = cx - (newimage.getWidth()*selectedGlass.ratioX);
            float dy = cy  - (newimage.getHeight()*selectedGlass.ratioY);

            mat.postTranslate(dx, dy);
            mat.postRotate(rotate, cx, cy);
            canvas.drawBitmap(newimage, mat, paint);
        }
        // =============== HATS RENDER ======================= //
        float ratio = (float)bitmapHats.getHeight() / (float)bitmapHats.getWidth();
        float w = faceWidth*selectedHats.width;
        float h = w*ratio;
        Bitmap newimage = Bitmap.createScaledBitmap(bitmapHats, (int)w, (int)h, true);

        Matrix mat = new Matrix();
        mat.reset();
        float dx = cx - (newimage.getWidth()*selectedHats.ratioX);
        float dy = cy  - (newimage.getHeight()*selectedHats.ratioY);

        mat.postTranslate(dx, dy);
        mat.postRotate(rotate, cx, cy);
        canvas.drawBitmap(newimage, mat, paint);

        selectedHats.lastAngle = rotate;
        selectedHats.lastWidth = faceWidth;
        selectedHats.lastX = cx;
        selectedHats.lastY = cy;

//        canvas.drawText("X: " + cx, cx - ID_X_OFFSET*6, cy - ID_Y_OFFSET*2, mIdPaint);
//        canvas.drawText("Y: " + cy, cx - ID_X_OFFSET*6, cy - ID_Y_OFFSET, mIdPaint);
//        canvas.drawText("angle: " + rotate, cx - ID_X_OFFSET*6, cy , mIdPaint);
//        canvas.drawText("width: " + faceWidth, cx - ID_X_OFFSET*6, cy + ID_Y_OFFSET, mIdPaint);
//        canvas.drawText("smile: " + String.format("%.2f", face.getIsSmilingProbability()), cx - ID_X_OFFSET*6, cy + ID_Y_OFFSET*2, mIdPaint);
//        canvas.drawLine(cx,cy,cx - ID_X_OFFSET*5, cy - ID_Y_OFFSET*2, mIdPaint);

        //  canvas.drawText("Угол поворота: " + String.format("%.2f", face.getEulerZ()), cx - ID_X_OFFSET, cy - ID_Y_OFFSET, mIdPaint);
        // canvas.drawText("Улыбка: " + String.format("%.2f", face.getIsSmilingProbability()), cx - ID_X_OFFSET, cy - ID_Y_OFFSET, mIdPaint);

        // Draws an oval around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = cx - xOffset;
        float top = cy - yOffset;
        float right = cx + xOffset;
        float bottom = cy + yOffset;
//
        canvas.drawOval(left, top, right, bottom, mBoxPaint);
        // if(face.getIsSmilingProbability() >= 0.9f){ context.takePicture();        }

    }
}
