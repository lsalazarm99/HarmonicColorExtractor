package com.apitiphy.harmoniccolorextractor;

public class HarmonicColors {

    private int backgroundColor, firstForegroundColor, secondForegroundColor;

    public HarmonicColors(int backgroundColor, int firstForegroundColor, int secondForegroundColor) {
        this.backgroundColor = backgroundColor;
        this.firstForegroundColor = firstForegroundColor;
        this.secondForegroundColor = secondForegroundColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getFirstForegroundColor() {
        return firstForegroundColor;
    }

    public int getSecondForegroundColor() {
        return secondForegroundColor;
    }
}
