/*
 * Copyright (C) 2018 SoftBank Robotics Europe
 * See COPYING for the licence
 */
package com.example.android.tflitecamerademo.utils;

/**
 * The constant class for TensorFlow
 */
public class Constant {
    public static final int INPUT_SIZE = 224;
    public static final int IMAGE_MEAN = 117;
    public static final float IMAGE_STD = 1;
    public static final String INPUT_NAME = "input";
    public static final String OUTPUT_NAME = "output";
    public static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    public static final String LABEL_FILE = "file:///android_asset/tensorflow_label_strings.txt";
}
