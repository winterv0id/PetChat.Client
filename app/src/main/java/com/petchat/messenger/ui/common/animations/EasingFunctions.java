package com.petchat.messenger.ui.common.animations;

import android.view.animation.Interpolator;

public class EasingFunctions {
    //https://easings.net/#

    // region Math constants
    private static final float c1 = 1.70158f;
    private static final float c2 = c1 * 1.525f;
    private static final float c3 = c1 + 1f;
    private static final float c4 = (float)((2 * Math.PI) / 3);
    private static final float c5 = (float)((2 * Math.PI) / 4.5);
    //endregion


    // region EaseQuint
    public static class EaseInQuintInterpolator implements Interpolator {
        public float getInterpolation(float x) { return x * x * x * x * x; }
    }
    public static class EaseOutQuintInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(1 - Math.pow(1 - x, 5)); }
    }
    public static class EaseInOutQuintInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x < 0.5 ? 16 * x * x * x * x * x : 1 - Math.pow(-2 * x + 2, 5) / 2); }
    }
    //endregion

    // region EaseCirc
    public static class EaseInCircInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(1 - Math.sqrt(1 - Math.pow(x, 2))); }
    }
    public static class EaseOutCircInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(Math.sqrt(1 - Math.pow(x - 1, 2))); }
    }
    public static class EaseInOutCircInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2 : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2); }
    }
    //endregion

    // region EaseElastic
    public static class EaseInElasticInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x == 0 ? 0 : x == 1 ? 1 : -Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * c4)); }
    }
    public static class EaseOutElasticInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x == 0 ? 0 : x == 1 ? 1 : Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1); }
    }
    public static class EaseInOutElasticInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * c5)) / 2 : (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * c5)) / 2 + 1); }
    }
    //endregion

    // region EaseQuad
    public static class EaseInQuadInterpolator implements Interpolator {
        public float getInterpolation(float x) { return x * x; }
    }
    public static class EaseOutQuadInterpolator implements Interpolator {
        public float getInterpolation(float x) { return 1 - (1 - x) * (1 - x); }
    }
    public static class EaseInOutQuadInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2); }
    }
    //endregion

    // region EaseQuart
    public static class EaseInQuartInterpolator implements Interpolator {
        public float getInterpolation(float x) { return x * x * x * x; }
    }
    public static class EaseOutQuartInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(1 - Math.pow(1 - x, 4)); }
    }
    public static class EaseInOutQuartInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x < 0.5 ? 8 * x * x * x * x : 1 - Math.pow(-2 * x + 2, 4) / 2); }
    }
    //endregion

    // region EaseExpo
    public static class EaseInExpoInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x == 0 ? 0 : Math.pow(2, 10 * x - 10)); }
    }
    public static class EaseOutExpoInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x == 1 ? 1 : 1 - Math.pow(2, -10 * x)); }
    }
    public static class EaseInOutExpoInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? Math.pow(2, 20 * x - 10) / 2 : (2 - Math.pow(2, -20 * x + 10)) / 2); }
    }
    //endregion

    // region EaseBack
    public static class EaseInBackInterpolator implements Interpolator {
        public float getInterpolation(float x) { return c3 * x * x * x - c1 * x * x; }
    }
    public static class EaseOutBackInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2)); }
    }
    public static class EaseInOutBackInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x < 0.5 ? (Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2 : (Math.pow(2 * x - 2, 2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2); }
    }
    //endregion

    // region EaseBounce
    public static class EaseInBounceInterpolator implements Interpolator {
        public float getInterpolation(float x) { return 1 - EaseOutBounceInterpolator.ease(1 - x); }
    }
    public static class EaseOutBounceInterpolator implements Interpolator {
        public float getInterpolation(float x) { return  ease(x); }

        public static float ease(float x){
            final float n1 = 7.5625f;
            final float d1 = 2.75f;

            if (x < 1 / d1) return n1 * x * x;
            else if (x < 2 / d1) return (float)(n1 * (x -= 1.5f / d1) * x + 0.75);
            else if (x < 2.5 / d1) return (float)(n1 * (x -= 2.25f / d1) * x + 0.9375);
            else return (float)(n1 * (x -= 2.625f / d1) * x + 0.984375);
        }
    }
    public static class EaseInOutBounceInterpolator implements Interpolator {
        public float getInterpolation(float x) { return x < 0.5 ? (1 - EaseOutBounceInterpolator.ease(1 - 2 * x)) / 2 : (1 + EaseOutBounceInterpolator.ease(2 * x - 1)) / 2; }
    }
    //endregion

    // region EaseCubic
    public static class EaseCubicInInterpolator implements Interpolator {
        public float getInterpolation(float x) { return  x * x * x; }
    }
    public static class EaseCubicOutInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(Math.pow(x - 1.0f, 3.0f) + 1.0f); }
    }
    public static class EaseCubicInOutInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (float)(x < 0.5f ? Math.pow(x * 2.0f, 3.0f) / 2.0f : (Math.pow((x - 1) * 2.0f, 3.0f) + 2.0f) / 2.0f); }
    }
    //endregion

    // region EaseSpring
    public static class EaseSpringInInterpolator implements Interpolator {
        public float getInterpolation(float x) { return  x * x * ((1.70158f + 1) * x - 1.70158f); }
    }
    public static class EaseSpringOutInterpolator implements Interpolator {
        public float getInterpolation(float x) { return (x - 1) * (x - 1) * ((1.70158f + 1) * (x - 1) + 1.70158f) + 1; }
    }
    //endregion
}