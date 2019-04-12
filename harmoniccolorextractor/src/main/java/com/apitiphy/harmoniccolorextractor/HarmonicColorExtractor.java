package com.apitiphy.harmoniccolorextractor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.graphics.Palette;

import java.util.List;

import static android.support.v4.graphics.ColorUtils.HSLToColor;
import static android.support.v4.graphics.ColorUtils.LABToColor;
import static android.support.v4.graphics.ColorUtils.calculateContrast;
import static android.support.v4.graphics.ColorUtils.calculateLuminance;
import static android.support.v4.graphics.ColorUtils.colorToHSL;
import static android.support.v4.graphics.ColorUtils.colorToLAB;

public class HarmonicColorExtractor {

    /**
     * The fraction below which we select the vibrant instead of the light/dark vibrant color
     */
    private static final float POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f;
    /**
     * Minimum saturation that a muted color must have if there exists if deciding between two
     * colors
     */
    private static final float MIN_SATURATION_WHEN_DECIDING = 0.19f;
    /**
     * Minimum fraction that any color must have to be picked up as a text color
     */
    private static final double MINIMUM_IMAGE_FRACTION = 0.002;
    /**
     * The population fraction to select the dominant color as the text color over a the colored
     * ones.
     */
    private static final float POPULATION_FRACTION_FOR_DOMINANT = 0.01f;
    /**
     * The population fraction to select a white or black color as the background over a color.
     */
    private static final float POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f;
    private static final float BLACK_MAX_LIGHTNESS = 0.08f;
    private static final float WHITE_MIN_LIGHTNESS = 0.90f;
    private static final int RESIZE_BITMAP_AREA = 150 * 150;
    private float[] mFilteredBackgroundHsl = null;
    private Palette.Filter mBlackWhiteFilter = (rgb, hsl) -> !isWhiteOrBlack(hsl);

    public class Builder {
        Bitmap bitmap;
        int[] backgroundRegion;
        int[] foregroundRegion;
        int resizeBitmapArea;

        public Builder(){}

        public Builder setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
            return this;
        }

        public Builder setBackgroundRegion(int left, int top, int right, int bottom) {
            this.backgroundRegion = new int[]{left, top, right, bottom};
            return this;
        }

        public Builder setForegroundRegion(int left, int top, int right, int bottom) {
            this.foregroundRegion = new int[]{left, top, right, bottom};
            return this;
        }

        public Builder setLeftSide() {
            return setLeftSide(0.4f);
        }

        public Builder setLeftSide(float textColorStartAreaFraction) {
            this.backgroundRegion = new int[]{0,0, bitmap.getWidth() / 2, bitmap.getHeight()};
            this.foregroundRegion = new int[]{(int) (bitmap.getWidth() * textColorStartAreaFraction), 0, bitmap.getWidth(), bitmap.getHeight()};
            return this;
        }

        public Builder setTopSide() {
            return setTopSide(0.4f);
        }

        public Builder setTopSide(float textColorStartAreaFraction) {
            this.backgroundRegion = new int[]{0, 0, bitmap.getWidth(), bitmap.getHeight() / 2};
            this.foregroundRegion = new int[]{0, (int) (bitmap.getHeight() * textColorStartAreaFraction), bitmap.getWidth(), bitmap.getHeight()};
            return this;
        }

        public Builder setRigtSide() {
            return setRightSide(0.4f);
        }

        public Builder setRightSide(float textColorStartAreaFraction) {
            this.backgroundRegion = new int[]{bitmap.getWidth() / 2, 0, bitmap.getWidth(), bitmap.getHeight()};
            this.foregroundRegion = new int[]{0, 0, (int) (bitmap.getWidth() * textColorStartAreaFraction), bitmap.getHeight()};
            return this;
        }

        public Builder setBottomSide() {
            return setBottomSide(0.4f);
        }

        public Builder setBottomSide(float textColorStartAreaFraction) {
            this.backgroundRegion = new int[]{0, bitmap.getHeight() / 2, bitmap.getWidth(), bitmap.getHeight()};
            this.foregroundRegion = new int[]{0, 0, bitmap.getWidth(), (int) (bitmap.getHeight() * textColorStartAreaFraction)};
            return this;
        }

        public Builder setResizeBitmapArea(int resizeBitmapArea) {
            this.resizeBitmapArea = resizeBitmapArea;
            return this;
        }

        public HarmonicColors getColors() {
            Palette.Builder paletteBuilder;
            Palette palette;
            int backgroundColor;

            paletteBuilder = Palette.from(bitmap);
            if(backgroundRegion != null) {
                paletteBuilder.setRegion(backgroundRegion[0], backgroundRegion[1], backgroundRegion[2], backgroundRegion[3]);
            }
            paletteBuilder.clearFilters();
            if(resizeBitmapArea != 0) {
                paletteBuilder.resizeBitmapArea(resizeBitmapArea);
            }

            palette = paletteBuilder.generate();

            backgroundColor = findBackgroundColorAndFilter(palette);

            if(foregroundRegion != null) {
                paletteBuilder.setRegion(foregroundRegion[0], foregroundRegion[1], foregroundRegion[2], foregroundRegion[3]);
            }

            if (mFilteredBackgroundHsl != null) {
                paletteBuilder.addFilter((rgb, hsl) -> {
                    // at least 10 degrees hue difference
                    float diff = Math.abs(hsl[0] - mFilteredBackgroundHsl[0]);
                    return diff > 10 && diff < 350;
                });
            }

            paletteBuilder.addFilter(mBlackWhiteFilter);
            palette = paletteBuilder.generate();
            int foregroundColorBase = selectForegroundColor(backgroundColor, palette);
            int[] foregroundColors = ensureColors(backgroundColor, foregroundColorBase);

            return new HarmonicColors(backgroundColor, foregroundColors[0], foregroundColors[1]);
        }
    }



    private int selectForegroundColor(int backgroundColor, Palette palette) {
        if (calculateLuminance(backgroundColor) > 0.5f) {
            return selectForegroundColorForSwatches(palette.getDarkVibrantSwatch(),
                    palette.getVibrantSwatch(),
                    palette.getDarkMutedSwatch(),
                    palette.getMutedSwatch(),
                    palette.getDominantSwatch(),
                    Color.BLACK);
        } else {
            return selectForegroundColorForSwatches(palette.getLightVibrantSwatch(),
                    palette.getVibrantSwatch(),
                    palette.getLightMutedSwatch(),
                    palette.getMutedSwatch(),
                    palette.getDominantSwatch(),
                    Color.WHITE);
        }
    }

    private int selectForegroundColorForSwatches(
            Palette.Swatch moreVibrant,
            Palette.Swatch vibrant,
            Palette.Swatch moreMutedSwatch,
            Palette.Swatch mutedSwatch,
            Palette.Swatch dominantSwatch,
            int fallbackColor) {
        Palette.Swatch coloredCandidate = selectVibrantCandidate(moreVibrant, vibrant);
        if (coloredCandidate == null) {
            coloredCandidate = selectMutedCandidate(mutedSwatch, moreMutedSwatch);
        }
        if (coloredCandidate != null) {
            if (dominantSwatch == coloredCandidate) {
                return coloredCandidate.getRgb();
            } else if ((float) coloredCandidate.getPopulation() / dominantSwatch.getPopulation() < POPULATION_FRACTION_FOR_DOMINANT
                    && dominantSwatch.getHsl()[1] > MIN_SATURATION_WHEN_DECIDING) {
                return dominantSwatch.getRgb();
            } else {
                return coloredCandidate.getRgb();
            }
        } else if (hasEnoughPopulation(dominantSwatch)) {
            return dominantSwatch.getRgb();
        } else {
            return fallbackColor;
        }
    }

    private Palette.Swatch selectMutedCandidate(Palette.Swatch first, Palette.Swatch second) {
        boolean firstValid = hasEnoughPopulation(first);
        boolean secondValid = hasEnoughPopulation(second);
        if (firstValid && secondValid) {
            float firstSaturation = first.getHsl()[1];
            float secondSaturation = second.getHsl()[1];
            float populationFraction = first.getPopulation() / (float) second.getPopulation();
            if (firstSaturation * populationFraction > secondSaturation) {
                return first;
            } else {
                return second;
            }
        } else if (firstValid) {
            return first;
        } else if (secondValid) {
            return second;
        }
        return null;
    }

    private Palette.Swatch selectVibrantCandidate(Palette.Swatch first, Palette.Swatch second) {
        boolean firstValid = hasEnoughPopulation(first);
        boolean secondValid = hasEnoughPopulation(second);
        if (firstValid && secondValid) {
            int firstPopulation = first.getPopulation();
            int secondPopulation = second.getPopulation();
            if (firstPopulation / (float) secondPopulation < POPULATION_FRACTION_FOR_MORE_VIBRANT) {
                return second;
            } else {
                return first;
            }
        } else if (firstValid) {
            return first;
        } else if (secondValid) {
            return second;
        }
        return null;
    }

    private boolean hasEnoughPopulation(Palette.Swatch swatch) {
        // We want a fraction that is at least 1% of the bitmap
        return swatch != null && (swatch.getPopulation() / (float) RESIZE_BITMAP_AREA > MINIMUM_IMAGE_FRACTION);
    }

    private int findBackgroundColorAndFilter(Palette palette) {
        // by default we use the dominant palette
        Palette.Swatch dominantSwatch = palette.getDominantSwatch();
        if (dominantSwatch == null) {
            // We're not filtering on white or black
            this.mFilteredBackgroundHsl = null;
            return Color.WHITE;
        }
        if (!isWhiteOrBlack(dominantSwatch.getHsl())) {
            this.mFilteredBackgroundHsl = dominantSwatch.getHsl();
            return dominantSwatch.getRgb();
        }
        // Oh well, we selected black or white. Lets look at the second color!
        List<Palette.Swatch> swatches = palette.getSwatches();
        float highestNonWhitePopulation = -1;
        Palette.Swatch second = null;
        for (Palette.Swatch swatch : swatches) {
            if (swatch != dominantSwatch
                    && swatch.getPopulation() > highestNonWhitePopulation
                    && !isWhiteOrBlack(swatch.getHsl())) {
                second = swatch;
                highestNonWhitePopulation = swatch.getPopulation();
            }
        }
        if (second == null) {
            // We're not filtering on white or black
            mFilteredBackgroundHsl = null;
            return dominantSwatch.getRgb();
        }
        if (dominantSwatch.getPopulation() / highestNonWhitePopulation > POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
            // The dominant swatch is very dominant, lets take it!
            // We're not filtering on white or black
            mFilteredBackgroundHsl = null;
            return dominantSwatch.getRgb();
        } else {
            mFilteredBackgroundHsl = second.getHsl();
            return second.getRgb();
        }
    }

    private boolean isWhiteOrBlack(float[] hsl) {
        return isBlack(hsl) || isWhite(hsl);
    }

    /**
     * @return true if the color represents a color which is close to black.
     */
    private boolean isBlack(float[] hslColor) {
        return hslColor[2] <= BLACK_MAX_LIGHTNESS;
    }

    /**
     * @return true if the color represents a color which is close to white.
     */
    private boolean isWhite(float[] hslColor) {
        return hslColor[2] >= WHITE_MIN_LIGHTNESS;
    }



    /**
     * The lightness difference that has to be added to the primary text color to obtain the
     * secondary text color when the background is light.
     */
    private static final int LIGHTNESS_TEXT_DIFFERENCE_LIGHT = 20;
    /**
     * The lightness difference that has to be added to the primary text color to obtain the
     * secondary text color when the background is dark.
     * A bit less then the above value, since it looks better on dark backgrounds.
     */
    private static final int LIGHTNESS_TEXT_DIFFERENCE_DARK = -10;

    private int[] ensureColors(int backgroundColor, int mForegroundColor) {
        //int backgroundColor = getBackgroundColor();
        int mPrimaryTextColor;
        int mSecondaryTextColor;

        double backLum = calculateLuminance(backgroundColor);
        double textLum = calculateLuminance(mForegroundColor);
        double contrast = calculateContrast(mForegroundColor, backgroundColor);
        // We only respect the given colors if worst case Black or White still has
        // contrast
        boolean backgroundLight = backLum > textLum && satisfiesTextContrast(backgroundColor, Color.BLACK) || backLum <= textLum && !satisfiesTextContrast(backgroundColor, Color.WHITE);
        if (contrast < 4.5f) {
            if (backgroundLight) {
                mSecondaryTextColor = findContrastColor(
                        mForegroundColor,
                        backgroundColor
                        /* findFG */
                );
                mPrimaryTextColor = changeColorLightness(
                        mSecondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_LIGHT);
            } else {
                mSecondaryTextColor = findContrastColorAgainstDark(
                        mForegroundColor,
                        backgroundColor
                        /* findFG */
                );
                mPrimaryTextColor = changeColorLightness(mSecondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_DARK);
            }
        } else {
            mPrimaryTextColor = mForegroundColor;
            mSecondaryTextColor = changeColorLightness(mPrimaryTextColor, backgroundLight ? LIGHTNESS_TEXT_DIFFERENCE_LIGHT : LIGHTNESS_TEXT_DIFFERENCE_DARK);
            if (calculateContrast(mSecondaryTextColor, backgroundColor) < 4.5f) {
                // oh well the secondary is not good enough
                if (backgroundLight) {
                    mSecondaryTextColor = findContrastColor(
                            mSecondaryTextColor,
                            backgroundColor
                            /* findFG */
                    );
                } else {
                    mSecondaryTextColor = findContrastColorAgainstDark(
                            mSecondaryTextColor,
                            backgroundColor
                            /* findFG */
                    );
                }
                mPrimaryTextColor = changeColorLightness(mSecondaryTextColor, backgroundLight ? -LIGHTNESS_TEXT_DIFFERENCE_LIGHT : -LIGHTNESS_TEXT_DIFFERENCE_DARK);
            }
        }

        return new int[]{mPrimaryTextColor, mSecondaryTextColor};

    }

    private static boolean satisfiesTextContrast(int backgroundColor, int foregroundColor) {
        return calculateContrast(foregroundColor, backgroundColor) >= 4.5;
    }

    private static int findContrastColor(int color, int other) {
        int fg = color;
        if (calculateContrast(fg, other) >= (double) 4.5f) {
            return color;
        }
        double[] lab = new double[3];
        colorToLAB(fg, lab);
        double low = 0, high = lab[0];
        final double a = lab[1], b = lab[2];
        for (int i = 0; i < 15 && high - low > 0.00001; i++) {
            final double l = (low + high) / 2;
            fg = LABToColor(l, a, b);
            if (calculateContrast(fg, other) > (double) 4.5f) {
                low = l;
            } else {
                high = l;
            }
        }
        return LABToColor(low, a, b);
    }

    private static int changeColorLightness(int baseColor, int amount) {
        final double[] result = getTempDouble3Array();
        colorToLAB(baseColor, result);
        result[0] = Math.max(Math.min(100, result[0] + amount), 0);
        return LABToColor(result[0], result[1], result[2]);
    }

    private static final ThreadLocal<double[]> TEMP_ARRAY = new ThreadLocal<>();

    private static double[] getTempDouble3Array() {
        double[] result = TEMP_ARRAY.get();
        if (result == null) {
            result = new double[3];
            TEMP_ARRAY.set(result);
        }
        return result;
    }

    private static int findContrastColorAgainstDark(int color, int other) {
        int fg = color;
        if (calculateContrast(fg, other) >= (double) 4.5f) {
            return color;
        }
        float[] hsl = new float[3];
        colorToHSL(fg, hsl);
        float low = hsl[2], high = 1;
        for (int i = 0; i < 15 && high - low > 0.00001; i++) {
            final float l = (low + high) / 2;
            hsl[2] = l;
            fg = HSLToColor(hsl);
            if (calculateContrast(fg, other) > (double) 4.5f) {
                high = l;
            } else {
                low = l;
            }
        }
        return fg;
    }

}

