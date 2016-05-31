/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.analysis.th;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.BreakIterator;
import java.util.HashSet;
import java.util.Locale;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.SegmentingTokenizerBase;
import org.apache.lucene.util.AttributeFactory;

/**
 * Tokenizer that use {@link BreakIterator} to tokenize Thai text.
 * <p>
 * WARNING: this tokenizer may not be supported by all JREs. It is known to work
 * with Sun/Oracle and Harmony JREs. If your application needs to be fully
 * portable, consider using ICUTokenizer instead, which uses an ICU Thai
 * BreakIterator that will always be available.
 */
public class ThaiTokenizer extends SegmentingTokenizerBase {

	// เพิ่มเข้ามา
	private static HashSet<String> lexitron = new HashSet<String>();;
	private LexTo lex = null;
	private int position;

	// อ่าน Dict เข้าใน HashSet ก่อน เพราะจะได้อ่านจากไฟล์แค่ทีเดียว
	static {
		try {
			InputStream fis = Test.class.getResourceAsStream("/org/apache/lucene/analysis/th/lexitron.txt");// new
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line;
			while ((line = br.readLine()) != null) {
				lexitron.add(line);
			}
			br.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * True if the JRE supports a working dictionary-based breakiterator for
	 * Thai. If this is false, this tokenizer will not work at all!
	 */
	public static final boolean DBBI_AVAILABLE;
	private static final BreakIterator proto = BreakIterator.getWordInstance(new Locale("th"));
	static {
		proto.setText("ภาษาไทย");
		DBBI_AVAILABLE = proto.isBoundary(4);
	}

	/** used for breaking the text into sentences */
	private static final BreakIterator sentenceProto = BreakIterator.getSentenceInstance(Locale.ROOT);

	int sentenceStart;
	int sentenceEnd;

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	public ThaiTokenizer() {
		this(DEFAULT_TOKEN_ATTRIBUTE_FACTORY);
	}

	// แก้โค้ดให้ไปเรียก method init() เพื่อใช้สร้าง instance LexTo
	/** Creates a new ThaiTokenizer, supplying the AttributeFactory */
	public ThaiTokenizer(AttributeFactory factory) {
		super(factory, (BreakIterator) sentenceProto.clone());
		if (!DBBI_AVAILABLE) {
			throw new UnsupportedOperationException("This JRE does not have support for Thai segmentation");
		}

		init();
	}

	// ใช้ Dict ที่อ่านมาได้ ยัดใส่ Instance ของ LexTo
	public void init() {
		try {
			this.lex = new LexTo(lexitron);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ในทุกๆ Sentence ที่ตัดออกมาโดย sentenceProto ยัดข้อความใส่ใน LexTo
	@Override
	protected void setNextSentence(int sentenceStart, int sentenceEnd) {
		this.sentenceStart = sentenceStart;
		this.sentenceEnd = sentenceEnd;

		// อ่านข้อความของ Sentence นั้น เข้า buffer
		String str = "";
		for (int i = sentenceStart; i < sentenceEnd; i++) {
			str += buffer[i];
		}

		// ยัดใส่ LexTo
		this.lex.wordInstance(str);
		
		//ตั้งค่าตำแหน่ง word term แรกใน sentence 
		this.position = this.lex.first();
	}

	@Override
	protected boolean incrementWord() {

		//เช็คว่ามีคำที่ตัดออกมาได้เหลืออยู่ไหม  ถ้าไม่่มีก็ return false เป็นการจบการตัดคำ
		if (this.lex.hasNext()) {
			
			//ค่าตำแหน่งอักขระเริ่มต้นของคำ
			int start = this.position;
			
			//ค่าตำแหน่งอักขระสุดท้ายของคำที่ตัดออกมา
			
			int end = this.lex.next();
			
			clearAttributes();
			
			//อันนี้ของเดิม คือแกะเอาคำที่ตัดออกมา ยัดใส่ใน termAttribute ซึ่งเป็นผลลัพธ์ของ Tokenizer
			termAtt.copyBuffer(buffer, sentenceStart + start, end - start);
			offsetAtt.setOffset(correctOffset(offset + sentenceStart + start),
					correctOffset(offset + sentenceStart + end));


			//อัพเดตตำแหน่งเริ่มต้นใหม่
			this.position = end;
			return true;
		}

		return false;
	}
}
