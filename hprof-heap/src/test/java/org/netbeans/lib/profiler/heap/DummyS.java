package org.netbeans.lib.profiler.heap;

import java.util.Random;

public class DummyS {

	public String latinString = "This is vanila latin string";
	public String cyrillicString = "А это строка кирилицей";
	public String unicodeString;
	
	{
		Random rnd = new Random(1);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i != 8096; ++i) {
			while(true) {
				char ch = (char)rnd.nextInt(65536);
				Character.isDefined(ch);
				sb.append(ch);
				break;
			}
		}
		unicodeString = sb.toString();
	}
	
}
