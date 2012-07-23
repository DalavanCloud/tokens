package name.kazennikov.annotations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.kazennikov.tokens.AbstractToken;
import name.kazennikov.tokens.BaseToken;
import name.kazennikov.tokens.BaseTokenType;
import name.kazennikov.tokens.NLPTokenType;
import name.kazennikov.tokens.SimpleTokenizer;
import name.kazennikov.tokens.TextToken;
import name.kazennikov.tokens.TokenStream;
import name.kazennikov.tokens.TokenType;

import com.google.common.collect.Sets;

public class SentenceSplitter implements Annotator {
	public static final String SENT = "sent";
	
	Set<String> abbrev = Sets.newHashSet();
	boolean splitOnLower;
	
	public SentenceSplitter(Collection<String> abbrevs, boolean splitOnLower) {
		this.abbrev.addAll(abbrevs);
		this.splitOnLower = splitOnLower;
	}

	@Override
	public void annotate(Document doc) {
		AnnotationStream in = new AnnotationStream(null, doc.getAnnotations("token"));
		
		while(!in.isNull() && ((TokenType)in.current().getFeature("type")).is(BaseTokenType.WHITESPACE))
			in.next();

		int sentenceStart = in.pos();


		while(!in.isEmpty()) {
			if(isSentenceEnd(doc, in, splitOnLower)) {
				add(doc, in, sentenceStart, in.pos()); // TODO: trim
				
				
				while(!in.isNull()  && in.current().getFeature("type", TokenType.class).is(BaseTokenType.WHITESPACE))
					in.next();
				
				sentenceStart = in.pos();
			} else {
				in.next();
			}
		}
		
		if(sentenceStart < in.pos()) {
			add(doc, in, sentenceStart, in.size());
		}
	}
	
	public void add(Document d, AnnotationStream s, int start, int end) {
		while(s.isNull(s.get(start)) || s.get(start).getFeature("type", TokenType.class).is(BaseTokenType.SPACE))
			start++;
		
		while(s.isNull(s.get(end)) || s.get(end).getFeature("type", TokenType.class).is(BaseTokenType.SPACE))
			end--;
		
		
		Annotation a = new Annotation(SENT, s.get(start).getStart(), s.get(end).getEnd());
		d.addAnnotation(a);
	}

	/**
	 * Check if the token could be a possible EOS
	 */
	public static boolean isPossibleEOS(Document d, Annotation token) {
		if(!((TokenType)token.getFeature("type")).is(BaseTokenType.PUNC))
			return false;
		
		String value = token.getText(d);
		
		if(value.length() == 1 && (value.equals("!") || value.equals("?") || value.equals(".")))
			return true;
		
		return value.contains(".");
	}
	
	/**
	 * Check if current token is a sentence end token. Also, if the current token is 
	 * the end of sentence, then advance to the logical end of the sentence.
	 * The method also checks for abbreviations that couldn't mean EOS
	 * @param s token stream
	 * @param splitOnLower if true, sentence ends on punctuation mark even if the start of the
	 * next sentence in in lowercase
	 * @return true if current tokens end a sentence
	 */
	public boolean isSentenceEnd(Document d, AnnotationStream s, boolean splitOnLower) {
		if(!isPossibleEOS(d, s.current()))
			return false;
		
		Annotation prev = s.current(-1);
		Annotation next = s.current(1);
		
		if(s.isNull(next)) {
			s.next();
			return true;
		}
		
		// skip all punctuation after current position
		while(s.current().getFeature("type", TokenType.class).is(BaseTokenType.PUNC)) {
			s.next();
		}
		
		if(!s.current().getFeature("type", TokenType.class).is(BaseTokenType.WHITESPACE))
			return false;
		
		int sentStart = s.pos();
		// skip all non-text after the punctuation
		while(s.isNull(s.get(sentStart)) && !s.get(sentStart).getFeature("type", TokenType.class).is(BaseTokenType.TEXT)) {
			sentStart++;
		}
		
		if(s.isNull(s.get(sentStart))) {
			s.setPos(sentStart);
			return true;
		}
		
		if(!Character.isLowerCase(s.get(sentStart).getText(d).charAt(0)) || splitOnLower) {
			if(isAbbrev(d, prev) || isInitial(d, prev))
				return false;
			return true;
		}
		
		
		
		
		return false;
		
	}

	/**
	 * Checks if the token could be an initial like A. A. Petrov
	 * @param token token to check
	 */
	private boolean isInitial(Document d, Annotation token) {
		String value = token.getText(d);
		if(value.isEmpty())
			return false;
		
		return value.length() < 3 && Character.isUpperCase(value.charAt(0));
	}

	private boolean isAbbrev(Document d, Annotation prev) {
		return abbrev.contains(prev.getText(d));
	}
	
	/**
	 * Load abbreviations from ETAP-3 definition file
	 * @param fileName file name 
	 * @return set of abbreviations
	 * @throws IOException
	 */
	public static Set<String> loadAbbrev(String fileName) throws IOException {
		HashSet<String> abbrev = new HashSet<String>();
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(fileName));
			String s;
			do {
				s = r.readLine();
				abbrev.add(s.trim());
			} while(s != null);
		} finally {
			if(r != null)
				r.close();
		}
		
		return abbrev;
		
	}
	
	public static void main(String[] args) {
		String s = "Мама мыла раму. Это 2.5 предложения.";
		Document d = new Document(s);
		Annotator tok = new Tokenizer();
		Annotator ss = new SentenceSplitter(new ArrayList<String>(), false);
		tok.annotate(d);
		ss.annotate(d);
		
		for(Annotation a : d.getAnnotations(SENT)) {
			System.out.printf("'%s' %s%n", a.getText(d), a.getFeatureMap());
		}

		
	}



}