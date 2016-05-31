package org.apache.lucene.analysis.th;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class Test {
	public static void main(String[] args) throws IOException {

		String input = "ข้าวสารในสต๊อกของโครงการรับจำนำข้าว ที่รัฐบาลยิ่งลักษณ์ใช้เงินภาษีของประชาชนไปซื้อข้าวในราคาแพงเก็บสต๊อกไว้จำนวนมาก ปัจจุบันยังมีข้าวสารเหลืออยู่ในสต๊อกถึง 11.4 ล้านตัน";
		//input = "Applications that build their search capabilities upon Lucene may support documents in various formats. ";

		Analyzer a = new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String arg0) {
				final Tokenizer source = new ThaiTokenizer();
				TokenStream result = new LowerCaseFilter(source);
				return new TokenStreamComponents(source, result);
			}
		};
		
		for(String s: tokenize(a, input)){
			System.out.println(s);
		}
	}

	public static ArrayList<String> tokenize(Analyzer ana, String input) {
		ArrayList<String> output = new ArrayList<>();
		TokenStream stream = ana.tokenStream(null, new StringReader(input));
		try {
			stream.reset();
			while(stream.incrementToken()){
				String token = stream.getAttribute(CharTermAttribute.class).toString();
				output.add(token.trim() + "\t" + token.trim().length());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return output;
	}
	

}
