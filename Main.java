package rar_test;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String[] toTest = new String[] {
            "C:\\TEST\\1.rar",
            "C:\\TEST\\1_pass.rar",
            "C:\\TEST\\1_pass_hide.rar"
        };
        String[] pass = new String[] {"plooop", "plop"};
        for(String tmp : toTest)
            test(tmp, pass);
    }

    public static void test(String totest, String[] passToTest){
        RAR_pass test = new RAR_pass(totest);
        System.out.println("Testing for : " + totest + " ...");
        if(test.passType == -1)
            System.out.println("This is not a valid rar.");
        else if(test.passType == 0)
            System.out.println("There is no password.");
        else{
            System.out.println("Password mode : " +
                    (test.passType == 1 ? "HEADER" : "FILE"));
            String pass = test.tryPassList(passToTest);
            if(pass == null)
                System.out.println("Password not found.");
            else
                System.out.println("Password : " + pass);
        }
        System.out.println();
    }
}
