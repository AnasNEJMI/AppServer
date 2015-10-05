package com.duster.fr.datasender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Anas on 25/09/2015.
 */

// J'ai compilé le code suivant, je vois la différence entre Arrays.toString() et new String()
public class Test {
        public static void main(String[] arg) throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            byte[] b = new byte[]{100,25,9,20,15};

            String s = b.toString();
            System.out.println(Arrays.toString(b)); // resultat : [112, 111]
            System.out.println(s); // resultat : po

    }
}
