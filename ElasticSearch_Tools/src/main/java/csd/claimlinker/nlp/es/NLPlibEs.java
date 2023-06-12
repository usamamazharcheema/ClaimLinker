package csd.claimlinker.nlp.es;

import com.yahoo.semsearch.fastlinking.hash.QuasiSuccinctEntityHash;
import csd.claimlinker.es.misc.ConsoleColor;
import csd.claimlinker.nlp.es.ArabicStemmer;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.io.BinIO;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.FileNotFoundException;

import org.tartarus.snowball.ext.PorterStemmer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * NLPlib is a wrapper class of StanfordNLP CoreNLP, providing an API handling CoreNLPs Annotators.
 */

public class NLPlibEs {
	private final StanfordCoreNLP master_pipeline;
	private CoreDocument doc;
	List<String> stopwords;
	List<String> punctuations;
	private final static boolean debug = false;
	protected QuasiSuccinctEntityHash quasiSuccinctEntityHash;

	public NLPlibEs(String stopwords_path, String puncs_path, String Hash_Path) throws IOException, ClassNotFoundException {
		System.out.println("NLPlib initializing ...");
		this.quasiSuccinctEntityHash = (QuasiSuccinctEntityHash) BinIO.loadObject(Hash_Path);
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner"); // not enough memory
		this.initStopword(stopwords_path);
		this.initPuncs(puncs_path);
		master_pipeline = new StanfordCoreNLP(props);
		System.out.println("NLPlibEs initialization finished ...");
	}

	public List<String> getLemmas(CoreDocument a) {
		if (a == null) return null;
		this.NLPlib_annotate(a);
		return this.doc.tokens().stream()
				.map(CoreLabel::lemma)
				.collect(Collectors.toList());
	}

	public String getLemmas_toString(CoreDocument a) {
		AtomicReference<String> cleaned = new AtomicReference<>("");
		if (a == null) return "null";
		this.getLemmas(a).forEach(elem -> {
//            System.out.println(elem);
			cleaned.updateAndGet(v -> v + elem + " ");
		});
		System.out.println(ConsoleColor.ANSI_CYAN + "[NLPlibEs] INFO got lemmas" + ConsoleColor.ANSI_RESET);
		return cleaned.get();
	}

	public String getStemmed(CoreDocument a) {
		PorterStemmer ps = new PorterStemmer();
		if (a == null) return "null";
		String stemmed_str;
		ArrayList<String> stemmed = new ArrayList<>();
		a.tokens().forEach(token -> {
			ps.setCurrent(token.originalText());
			ps.stem();
			synchronized (stemmed) {
				stemmed.add(ps.getCurrent());
			}
		});

		stemmed_str = String.join(" ", stemmed);
		System.out.println(ConsoleColor.ANSI_CYAN + "[NLPlibEs] INFO stemmed" + ConsoleColor.ANSI_RESET);
		return stemmed_str;
	}
	
	public String getArabicStemmed(CoreDocument a) {
//		PorterStemmer ps = new PorterStemmer();
		ArabicStemmer stemmer=new ArabicStemmer();
		if (a == null) return "null";
		String aStemmed_str;
		ArrayList<String> stemmed = new ArrayList<>();
		a.tokens().forEach(token -> {
			
//			ps.setCurrent(token.originalText());
//			ps.stem();
			synchronized (stemmed) {
				
			stemmed.add(stemmer.stemWord(token.originalText()));
			
			}
			
		});

		aStemmed_str = String.join(" ", stemmed);
		System.out.println(ConsoleColor.ANSI_CYAN + "[NLPlib] INFO stemmed" + ConsoleColor.ANSI_RESET);
		return aStemmed_str;
	}
	

	public String getWithoutStopwords(CoreDocument a) {
		AtomicReference<String> cleaned = new AtomicReference<>("");
		if (a != null) this.setDoc(a);
		this.removeStopWords().forEach(elem -> {
//            System.out.println(elem);
			cleaned.updateAndGet(v -> v + elem.originalText() + " ");
		});
		
//        System.out.println(ConsoleColor.ANSI_CYAN + "[NLPlibEs] stopwords removed" + ConsoleColor.ANSI_RESET);
		return cleaned.get();
	}

	public String getWithoutPunctuations(CoreDocument a) {
		AtomicReference<String> cleaned = new AtomicReference<>("");
		if (a != null) this.setDoc(a);
		this.removePunctuations().forEach(elem -> {
//            System.out.println(elem);
			cleaned.updateAndGet(v -> v + elem.originalText() + " ");
		});
//        System.out.println(ConsoleColor.ANSI_CYAN + "[NLPlibEs] stopwords removed" + ConsoleColor.ANSI_RESET);
		return cleaned.get();
	}

  public String getWordnetExpansion(CoreDocument a) throws JWNLException   {
 

  //JWNL.initialize(new FileInputStream("D:\\Master\\Gesis\\amirProject\\ClaimLinker\\ClaimLinker_commons\\properties.xml"));
	  AtomicReference<String> cleaned = new AtomicReference<>("");


	  Dictionary dictionary = Dictionary.getDefaultResourceInstance();
	  MaxentTagger maxentTagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words-distsim.tagger");
	  String taggedQuery = maxentTagger.tagString(a.text());
      String[] eachTag = taggedQuery.split("\\s+");
      //System.out.println("Term      " + "Standford tag");
      System.out.println("----------------------------------");
      for (int i = 0; i < eachTag.length; i++) {
          String term = eachTag[i].split("_")[0];
          String tag = eachTag[i].split("_")[1];
          System.out.println( term + " " + tag);
          POS pos = getPos(tag);

          // Ignore anything that is not a noun, verb, adjective, adverb
          if(pos != null) {
              // Can get various synsets
              IndexWord iWord;
              iWord = dictionary.getIndexWord(pos, term);
              if(iWord != null) {
                  for (Synset synset : iWord.getSenses()) {
                      List<Word> words = synset.getWords();
                      for (Word word : words) {
                    	  cleaned.updateAndGet(v -> v + word.getLemma() + " ");
                    	  
                          //System.out.println(word.getLemma());
                      }
                  }
              }
          }
      }
      
  System.out.println(ConsoleColor.ANSI_CYAN + "[NLPlibEs] INFO WordNet Expansion applied" + ConsoleColor.ANSI_RESET);
  //System.out.println(cleaned.get());
  return cleaned.get();
  

}
  


	public CoreDocument getDoc() {
		return doc;
	}

	public CoreDocument NLPlib_annotate(String doc) {
		CoreDocument a = new CoreDocument(doc);
		return NLPlib_annotate(a);
	}

	public CoreDocument NLPlib_annotate(CoreDocument doc) {
		this.setDoc(doc);
		this.master_pipeline.annotate(doc);
		return doc;
	}

	public void setDoc(CoreDocument doc) {
		this.doc = doc;
	}
	 private static POS getPos(String taggedAs) {
	        switch(taggedAs) {
	            case "NN" :
	            case "NNS" :
	            case "NNP" :
	            case "NNPS" :
	                return POS.NOUN;
	            case "VB" :
	            case "VBD" :
	            case "VBG" :
	            case "VBN" :
	            case "VBP" :
	            case "VBZ" :
	                return POS.VERB;
	            case "JJ" :
	            case "JJR" :
	            case "JJS" :
	                return POS.ADJECTIVE;
	            case "RB" :
	            case "RBR" :
	            case "RBS" :
	                return POS.ADVERB;
	            default:
	                return null;
	        }
	 }

	public ArrayList<String> getAnnotationSentences(CoreDocument doc) {
		if (debug) System.out.println("= = =");
		if (debug) System.out.println("[NLPlibEs] Entities found");
		if (debug) System.out.println("= = =");
		boolean inEntity = false;
		int counter = 0;
		String currentEntity = "";
		String currentEntityType = "";
		ArrayList<String> tokens = new ArrayList<>();
		Annotation document = new Annotation(doc.annotation());
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		for (Object sentence : sentences) {
			if (debug) System.out.println("[NLPlibEs] Sentence #" + counter++);
			for (CoreLabel token : ((CoreMap) sentence).get(CoreAnnotations.TokensAnnotation.class)) {

				String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
				if (debug) System.out.println("[OT: " + token.originalText() + " ]");
				if (!inEntity) {
					if (!"O".equals(ne)) {
						inEntity = true;
						currentEntity = "";
						currentEntityType = ne;
					}
				}
				if (inEntity) {
					if ("O".equals(ne)) {
						inEntity = false;
						tokens.add(currentEntity);
//                        System.out.println("Extracted " + currentEntityType + " " + currentEntity.trim());

					} else {
						currentEntity += " " + token.originalText();
					}

				}

			}
		}

		return tokens;
	}


	public List<CoreLabel> removeStopWords() {
		int counter = 0;
		Annotation document = new Annotation(doc.annotation());
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		List<CoreLabel> without_stopwords = new ArrayList<>();
		for (Object sentence : sentences) {
			if (debug) System.out.println("[NLPlibEs] Sentence #" + counter++);
			for (CoreLabel token : ((CoreMap) sentence).get(CoreAnnotations.TokensAnnotation.class)) {
				if (debug) {
					System.out.printf("[NLPlibEs] Token : %15s - %15s\n", token, token.originalText());
				}
				if (!this.stopwords.contains(token.originalText()))
					without_stopwords.add(token);
				else if (debug)
					System.out.println(" <-");
				if (debug) System.out.print("\n");
			}
		}
		if (debug) System.out.println("[NLPlibEs] --------  ");
		
		return without_stopwords;
	}

	public List<CoreLabel> removePunctuations() {
		int counter = 0;
		Annotation document = new Annotation(doc.annotation());
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		List<CoreLabel> without_punctuations = new ArrayList<>();
		for (Object sentence : sentences) {
			if (debug) System.out.println("[NLPlibEs] Sentence #" + counter++);
			for (CoreLabel token : ((CoreMap) sentence).get(CoreAnnotations.TokensAnnotation.class)) {
				if (debug) {
					System.out.printf("[NLPlibEs] Token : %15s - %15s\n", token, token.originalText());
				}
				if (!this.punctuations.contains(token.originalText()))
					without_punctuations.add(token);
				else if (debug)
					System.out.println(" <-");
				if (debug) System.out.print("\n");
			}
		}
		if (debug) System.out.println("[NLPlibEs] --------  ");
		return without_punctuations;
	}

	private void initStopword(String file_path) {
		this.stopwords = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new FileReader(file_path))) {
			String input;
			while ((input = in.readLine()) != null) {
				this.stopwords.add(input);
			}
			System.out.println("Stopwords initialization finished!");
		} catch (IOException e) {
			System.err.println("Stopwords file error!");
			System.exit(1);
		}
	}

	private void initPuncs(String file_path) {
		this.punctuations = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new FileReader(file_path))) {
			String input;
			while ((input = in.readLine()) != null) {
				this.punctuations.add(input);
			}
			System.out.println("Punctuations initialization finished!");
		} catch (IOException e) {
			System.err.println("Punctuations file error!");
			System.exit(1);
		}
	}

}
