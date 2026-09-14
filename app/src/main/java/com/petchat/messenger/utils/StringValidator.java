package com.petchat.messenger.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringValidator {
    private final String value;
    private boolean isValidString = true;

    private static final String PASSWORD_SAFE_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    public StringValidator(String value){
        this.value = value;
    }
    public static StringValidator create(String value) {
        return new StringValidator(value);
    }

    public StringValidator notNullOrWhiteSpace()
    {
        isValidString &= !value.isBlank();
        return this;
    }
    public StringValidator notNullOrEmpty() {
        isValidString &= !(value == null || value.isEmpty());
        return this;
    }
    public StringValidator notEmpty() {
        isValidString &= !value.isEmpty();
        return this;
    }
    public StringValidator pattern(String pattern)
    {
        Matcher matcher = Pattern.compile(pattern).matcher(value);
        isValidString &= value.isEmpty() || matcher.find();
        return this;
    }
    public StringValidator isPasswordSafe() {
        return pattern(PASSWORD_SAFE_PATTERN);
    }
    public StringValidator aboveLength(int length)
    {
        isValidString &= value.length() > length;
        return this;
    }
    public StringValidator belowLength(int length)
    {
        isValidString &= value.length() < length;
        return this;
    }
    public StringValidator equalLength(int length)
    {
        isValidString &= value.length() == length;
        return this;
    }

    public boolean isValid() {
        return isValidString;
    };
}