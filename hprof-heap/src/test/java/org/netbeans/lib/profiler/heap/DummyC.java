package org.netbeans.lib.profiler.heap;

public class DummyC {

    Struct structField = new Struct();
    Struct[] structArray = {null, new Struct(), null, null};
    {
        structArray[1].textField = "this is struct #1";
    }

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
