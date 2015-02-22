/**
 * Copyright 2014 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.netbeans.lib.profiler.heap;

public class DummyC {

    Struct structField = new Struct();
    Struct[] structArray = {null, new Struct(), null, null};
    {
        structArray[1].textField = "this is struct #1";
    }

    boolean[] bool_values = {true, false};
    byte[] byte_values = {Byte.MIN_VALUE, Byte.MAX_VALUE};
    short[] short_values = {Short.MIN_VALUE, Short.MAX_VALUE};
    char[] char_values = {Character.MIN_VALUE, Character.MAX_VALUE};
    int[] int_values = {Integer.MIN_VALUE, Integer.MAX_VALUE};
    long[] long_values = {Long.MIN_VALUE, Long.MAX_VALUE};
    float[] float_values = {Float.MIN_VALUE, Float.NaN, Float.MAX_VALUE};
    double[] double_values = {Double.MIN_VALUE, Double.NaN, Double.MAX_VALUE};
    
    public static class Struct {

        boolean trueField = true;
        boolean falseField = false;
        byte byteField = 13;
        short shortField = -14;
        char charField = 15;
        int intField = 0x66666666;
        long longField = 0x6666666666l;
        float floatField = 0.1f;
        double doubleField = -0.2;

        Boolean trueBoxedField = true;
        Boolean falseBoxedField = false;
        Byte byteBoxedField = 13;
        Short shortBoxedField = -14;
        Character charBoxedField = 15;
        Integer intBoxedField = 0x66666666;
        Long longBoxedField = 0x6666666666l;
        Float floatBoxedField = 0.1f;
        Double doubleBoxedField = -0.2;

        String textField = "this is struct";
    }
}
